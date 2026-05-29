package com.odontoflow.repository;

import com.odontoflow.entity.ConversationMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    @Query("SELECT m FROM ConversationMessage m WHERE m.phone = :phone " +
           "AND m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<ConversationMessage> findRecent(
            @Param("phone") String phone,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );

    @Query("SELECT COUNT(m) FROM ConversationMessage m WHERE m.phone = :phone " +
           "AND m.role = 'user' AND m.createdAt >= :since")
    long countUserMessagesSince(
            @Param("phone") String phone,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(m) FROM ConversationMessage m WHERE m.phone = :phone " +
           "AND m.toolName = :toolName AND m.createdAt >= :since")
    long countByPhoneAndToolNameSince(
            @Param("phone") String phone,
            @Param("toolName") String toolName,
            @Param("since") LocalDateTime since
    );

    @Modifying
    @Query("DELETE FROM ConversationMessage m WHERE m.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
