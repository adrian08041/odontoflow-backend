package com.odontoflow.dto.response;

import com.odontoflow.entity.AuditLog;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.util.AuditSummary;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogDetailResponse(
        UUID id,
        String entity,
        UUID entityId,
        AuditAction action,
        UUID userId,
        String userName,
        String summary,
        String ipAddress,
        LocalDateTime timestamp,
        String changes
) {
    public static AuditLogDetailResponse from(AuditLog log) {
        return new AuditLogDetailResponse(
                log.getId(),
                log.getEntity(),
                log.getEntityId(),
                log.getAction(),
                log.getUserId(),
                log.getUserName(),
                AuditSummary.summarize(log.getEntity(), log.getChanges()),
                log.getIpAddress(),
                log.getTimestamp(),
                log.getChanges()
        );
    }
}
