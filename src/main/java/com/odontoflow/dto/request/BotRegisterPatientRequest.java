package com.odontoflow.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Payload do cadastro de lead via bot. phone é preenchido pelo n8n (Trigger), não pelo LLM. */
public record BotRegisterPatientRequest(
        @NotBlank String name,
        @NotBlank String cpf,
        @NotBlank String phone
) {}
