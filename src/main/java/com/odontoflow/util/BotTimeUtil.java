package com.odontoflow.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normaliza o horário pro formato HH:mm exigido pelos DTOs do bot. O AI Agent (LLM) costuma mandar o
 * time já com separador quando o paciente fala coloquial ("8 horas", "8h"):
 * "8" -> "08:00", "8h" -> "08:00", "8:45" -> "08:45", "8h30" -> "08:30", "08:00h" -> "08:00".
 * Formatos com separador textual sem dígitos colados (ex: "8 e 45") só capturam a hora -> "08:00"
 * (os minutos se perdem); o slot resultante, se não existir, é rejeitado depois por resolveSlotDate.
 * Valores irreconhecíveis seguem crus pro @Pattern do DTO rejeitar com mensagem clara.
 */
public final class BotTimeUtil {

    private BotTimeUtil() {}

    private static final Pattern COLLOQUIAL =
            Pattern.compile("^(\\d{1,2})\\s*[:hH]?\\s*(\\d{1,2})?");

    public static String normalizeTime(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        Matcher m = COLLOQUIAL.matcher(t);
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
