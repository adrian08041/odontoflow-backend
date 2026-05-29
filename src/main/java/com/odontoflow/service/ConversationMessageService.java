package com.odontoflow.service;

import com.odontoflow.entity.ConversationMessage;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMessageService {

    private static final int MEMORY_WINDOW_MIN = 30;
    private static final int BUFFER_LIMIT = 20;
    private static final int RATE_LIMIT_WINDOW_MIN = 5;
    private static final long RATE_LIMIT_MAX_MESSAGES = 10;
    private static final int RETENTION_DAYS = 30;
    private static final int MAX_CONTENT_CHARS = 2000;

    private final ConversationMessageRepository repository;

    @Transactional
    public ConversationMessage append(String phone, String role, String content, String toolName) {
        if (content != null && content.length() > MAX_CONTENT_CHARS) {
            throw new BusinessException("Conteúdo excede " + MAX_CONTENT_CHARS + " caracteres");
        }
        if (!List.of("user", "assistant", "tool").contains(role)) {
            throw new BusinessException("role inválido: deve ser user, assistant ou tool");
        }
        ConversationMessage msg = ConversationMessage.builder()
                .phone(phone)
                .role(role)
                .content(content)
                .toolName(toolName)
                .build();
        return repository.save(msg);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> listRecent(String phone) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(MEMORY_WINDOW_MIN);
        // Ordem DESC do repo, mas reverter pra ordem cronológica natural do LLM
        List<ConversationMessage> desc = repository.findRecent(
                phone, since, PageRequest.of(0, BUFFER_LIMIT));
        return desc.reversed();
    }

    @Transactional(readOnly = true)
    public long countUserMessagesInRateWindow(String phone) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(RATE_LIMIT_WINDOW_MIN);
        return repository.countUserMessagesSince(phone, since);
    }

    public boolean isRateLimited(String phone) {
        return countUserMessagesInRateWindow(phone) > RATE_LIMIT_MAX_MESSAGES;
    }

    @Transactional(readOnly = true)
    public boolean wasUnsupportedRepliedRecently(String phone) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(MEMORY_WINDOW_MIN);
        return repository.countByPhoneAndToolNameSince(
                phone, "unsupported_media_reply", since) > 0;
    }

    @Scheduled(cron = "0 5 2 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void purgeOldMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = repository.deleteOlderThan(cutoff);
        log.info("Purged {} conversation_messages older than {}", deleted, cutoff);
    }
}
