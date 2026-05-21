package com.odontoflow.controller;

import com.odontoflow.dto.response.AuditLogDetailResponse;
import com.odontoflow.dto.response.AuditLogResponse;
import com.odontoflow.dto.response.PageResponse;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Log", description = "Trilha de auditoria — imutável, apenas leitura, ADMIN only")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Listar logs de auditoria",
            description = "Lista paginada com filtros opcionais. Ordenado por timestamp DESC.")
    @ApiResponse(responseCode = "200", description = "Lista paginada de logs")
    @ApiResponse(responseCode = "403", description = "Acesso negado (somente ADMIN)")
    public ResponseEntity<PageResponse<AuditLogResponse>> findAll(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(
                auditLogService.findAll(entity, entityId, userId, action, startDate, endDate, pageable)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um log",
            description = "Retorna um log específico incluindo o campo `changes` completo")
    @ApiResponse(responseCode = "200", description = "Log encontrado")
    @ApiResponse(responseCode = "400", description = "Log não encontrado")
    @ApiResponse(responseCode = "403", description = "Acesso negado (somente ADMIN)")
    public ResponseEntity<AuditLogDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditLogService.findById(id));
    }
}
