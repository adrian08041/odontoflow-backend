package com.odontoflow.repository;

import com.odontoflow.entity.AuditLog;
import com.odontoflow.entity.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:entity IS NULL OR a.entity = :entity)
              AND (:entityId IS NULL OR a.entityId = :entityId)
              AND (:userId IS NULL OR a.userId = :userId)
              AND (:action IS NULL OR a.action = :action)
              AND (CAST(:startDate AS timestamp) IS NULL OR a.timestamp >= :startDate)
              AND (CAST(:endDate   AS timestamp) IS NULL OR a.timestamp <  :endDate)
            ORDER BY a.timestamp DESC, a.id DESC
            """)
    Page<AuditLog> findFiltered(
            @Param("entity")    String entity,
            @Param("entityId")  UUID entityId,
            @Param("userId")    UUID userId,
            @Param("action")    AuditAction action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate")   LocalDateTime endDate,
            Pageable pageable
    );
}
