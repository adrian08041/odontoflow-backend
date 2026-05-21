package com.odontoflow.dto.request;

import com.odontoflow.entity.enums.ReminderAction;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReminderResponseRequest(
        @NotNull ReminderAction action,
        LocalDateTime respondedAt
) {}
