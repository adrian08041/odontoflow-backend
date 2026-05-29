package com.odontoflow.dto.request;

import jakarta.validation.constraints.NotBlank;

public record HandoffRequest(
        @NotBlank String phone,
        @NotBlank String reason
) {}
