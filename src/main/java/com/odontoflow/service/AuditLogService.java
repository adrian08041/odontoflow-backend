package com.odontoflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.odontoflow.dto.response.AuditLogDetailResponse;
import com.odontoflow.dto.response.AuditLogResponse;
import com.odontoflow.entity.AuditLog;
import com.odontoflow.entity.User;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.AuditLogRepository;
import com.odontoflow.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final String SYSTEM_USER_NAME = "Sistema";

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager txManager;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private TransactionTemplate writeTx;

    @PostConstruct
    void initTxTemplate() {
        TransactionTemplate t = new TransactionTemplate(txManager);
        t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.writeTx = t;
    }

    /**
     * Grava um log de auditoria para o usuário atualmente autenticado.
     * Roda em transação isolada (REQUIRES_NEW) — falha do audit nunca afeta o caller.
     */
    public void log(String entity, UUID entityId, AuditAction action, Object changes) {
        UUID userId = currentUserId();
        String userName = currentUserName();
        if (userName == null && userId != null) {
            userName = resolveUserName(userId);
        }
        persist(entity, entityId, action, userId, userName, changes);
    }

    /**
     * Grava um log explicitando o usuário responsável. Usado quando o SecurityContext
     * ainda não foi populado (ex: LOGIN) ou para operações em nome de outro usuário.
     */
    public void logAs(UUID userId, String userName, String entity, UUID entityId,
                       AuditAction action, Object changes) {
        persist(entity, entityId, action, userId, userName, changes);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(String entity, UUID entityId, UUID userId,
                                          AuditAction action, LocalDateTime startDate,
                                          LocalDateTime endDate, Pageable pageable) {
        String entityParam = (entity == null || entity.isBlank()) ? null : entity;
        return auditLogRepository
                .findFiltered(entityParam, entityId, userId, action, startDate, endDate, pageable)
                .map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public AuditLogDetailResponse findById(UUID id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Log de auditoria não encontrado."));
        return AuditLogDetailResponse.from(log);
    }

    // ---------- internos ----------

    private void persist(String entity, UUID entityId, AuditAction action,
                         UUID userId, String userName, Object changes) {
        try {
            String safeUserName = truncate(userName != null ? userName : SYSTEM_USER_NAME, 120);
            AuditLog entry = AuditLog.builder()
                    .entity(entity)
                    .entityId(entityId != null ? entityId : nilUuid())
                    .action(action)
                    .userId(userId != null ? userId : nilUuid())
                    .userName(safeUserName)
                    .changes(serialize(changes))
                    .ipAddress(currentIp())
                    .build();

            writeTx.executeWithoutResult(status -> auditLogRepository.save(entry));
        } catch (Exception ex) {
            // Auditoria nunca deve quebrar o fluxo principal. Loga e segue.
            log.warn("Falha ao gravar AuditLog [entity={}, entityId={}, action={}]: {}",
                    entity, entityId, action, ex.getMessage());
        }
    }

    private String serialize(Object changes) {
        if (changes == null) return null;
        if (changes instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException ex) {
            log.warn("Falha ao serializar changes do AuditLog: {}", ex.getMessage());
            return null;
        }
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UUID uuid) return uuid;
        if (principal instanceof String s) {
            try { return UUID.fromString(s); } catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }

    /** Lê o userName do Authentication.details (setado pelo JwtAuthenticationFilter). Evita query extra. */
    private String currentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object details = auth.getDetails();
        return details instanceof String s && !s.isBlank() ? s : null;
    }

    private String resolveUserName(UUID userId) {
        return userRepository.findById(userId).map(User::getName).orElse(SYSTEM_USER_NAME);
    }

    /** IP de origem. Não confia em X-Forwarded-For (cliente pode forjar). */
    private String currentIp() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (!(attrs instanceof ServletRequestAttributes sra)) return null;
            HttpServletRequest req = sra.getRequest();
            String ip = req.getRemoteAddr();
            return normalizeIp(ip);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeIp(String ip) {
        if (ip == null) return null;
        return "0:0:0:0:0:0:0:1".equals(ip) ? "::1" : ip;
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    /**
     * UUID nulo (00000000-...-0) para colunas NOT NULL quando não há valor real:
     * userId em jobs scheduled/bootstrap, ou entityId em eventos sem entidade-alvo
     * (ex: handoff do bot WhatsApp, que registra uma conversa, não uma entidade).
     */
    private UUID nilUuid() {
        return new UUID(0L, 0L);
    }
}
