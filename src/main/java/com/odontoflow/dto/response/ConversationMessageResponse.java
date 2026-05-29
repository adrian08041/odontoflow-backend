package com.odontoflow.dto.response;

import com.odontoflow.entity.ConversationMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID id,
        String role,
        String content,
        String toolName,
        LocalDateTime createdAt
) {
    public static ConversationMessageResponse from(ConversationMessage m) {
        return new ConversationMessageResponse(
                m.getId(), m.getRole(), m.getContent(), m.getToolName(), m.getCreatedAt()
        );
    }
}
