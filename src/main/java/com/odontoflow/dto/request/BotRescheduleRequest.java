package com.odontoflow.dto.request;

import com.odontoflow.util.BotTimeUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Request do bot WhatsApp para REMARCAR a próxima consulta do paciente.
 * Identifica paciente por {@code phone} (injetado fixo pelo n8n) e dentista por {@code dentistName}
 * (lido do get_availability) — mesmo motivo do create (o LLM alucina UUIDs). type/procedure/notes
 * NÃO vêm: são preservados da consulta existente.
 */
public record BotRescheduleRequest(
        @NotBlank String phone,
        @NotBlank String dentistName,
        @NotNull LocalDate date,
        @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
                           message = "time deve estar em HH:mm") String time
) {
    public BotRescheduleRequest {
        time = BotTimeUtil.normalizeTime(time);
    }
}
