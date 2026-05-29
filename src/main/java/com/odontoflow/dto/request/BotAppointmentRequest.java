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
) {
    public BotAppointmentRequest {
        // O LLM às vezes manda a hora sem o zero à esquerda ("8:00" em vez de "08:00"),
        // sobretudo quando o paciente fala "8 horas". Como o compact constructor roda na
        // desserialização (antes do @Valid), normalizamos aqui e a validação @Pattern passa.
        if (time != null) {
            time = time.trim();
            if (time.matches("\\d:[0-5]\\d")) {
                time = "0" + time;
            }
        }
    }
}
