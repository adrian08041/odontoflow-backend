package com.odontoflow.dto.request;

import com.odontoflow.entity.enums.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Request do bot WhatsApp para criar consulta.
 * Identifica paciente por {@code phone} e dentista por {@code dentistName} (não por UUID):
 * o AI Agent (LLM) alucina identificadores opacos longos, mas acerta telefone (injetado fixo
 * pelo n8n) e nome do dentista (lido do get_availability). O backend resolve os UUIDs.
 */
public record BotAppointmentRequest(
        @NotBlank String phone,
        @NotBlank String dentistName,
        @NotNull LocalDate date,
        @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
                           message = "time deve estar em HH:mm") String time,
        @NotNull AppointmentType type,
        String procedure,
        String notes
) {}
