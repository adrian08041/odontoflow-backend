package com.odontoflow.dto.response;

import com.odontoflow.entity.AuditLog;
import com.odontoflow.entity.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String entity,
        UUID entityId,
        AuditAction action,
        UUID userId,
        String userName,
        String ipAddress,
        LocalDateTime timestamp
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getEntity(),
                log.getEntityId(),
                log.getAction(),
                log.getUserId(),
                log.getUserName(),
                log.getIpAddress(),
                log.getTimestamp()
        );
    }
}
