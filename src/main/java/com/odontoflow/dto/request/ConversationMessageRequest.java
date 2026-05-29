package com.odontoflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationMessageRequest(
        @NotBlank String role,
        @Size(max = 2000) String content,
        String toolName
) {}
