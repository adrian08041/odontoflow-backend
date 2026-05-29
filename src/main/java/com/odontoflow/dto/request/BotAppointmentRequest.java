package com.odontoflow.dto.request;

import com.odontoflow.entity.enums.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.regex.Matcher;

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
        time = normalizeTime(time);
    }

    /**
     * Normaliza o horário pro formato HH:mm exigido pelo @Pattern. O AI Agent (LLM) manda o time
     * de formas variadas quando o paciente fala coloquial ("8 horas", "8h", "8 e 45"):
     * "8" -> "08:00", "8h" -> "08:00", "8:0"/"8:45" -> "08:45", "8h30" -> "08:30", "08:00h" -> "08:00".
     * Roda no compact constructor (antes do @Valid), então a validação passa. Valores
     * irreconhecíveis seguem crus pro @Pattern rejeitar com mensagem clara.
     */
    private static String normalizeTime(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        Matcher m = java.util.regex.Pattern.compile("^(\\d{1,2})\\s*[:hH]?\\s*(\\d{1,2})?").matcher(t);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            int min = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            if (h <= 23 && min <= 59) {
                return String.format("%02d:%02d", h, min);
            }
        }
        return t;
    }
}
