package com.odontoflow.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Gera resumo legível ("summary") de um log de auditoria a partir do JSON `changes`.
 * Mantém em sincronia com {@code lib/utils/audit-helpers.ts} do front — pequenas
 * divergências aceitáveis, mas mudanças estruturais devem ser refletidas dos dois lados.
 */
public final class AuditSummary {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private AuditSummary() {}

    /**
     * Resume em uma frase curta o conteúdo do log. Retorna {@code null} quando
     * não há informação contextual confiável (entidade desconhecida, JSON inválido,
     * snapshot vazio, etc).
     */
    public static String summarize(String entity, String changesJson) {
        if (entity == null || changesJson == null || changesJson.isBlank()) return null;

        JsonNode root;
        try {
            root = MAPPER.readTree(changesJson);
        } catch (Exception ex) {
            return null;
        }

        JsonNode snap = pickSnapshot(root);
        if (snap == null || !snap.isObject()) return null;

        return switch (entity) {
            case "Patient", "Dentist", "Insurance" -> str(snap.get("name"));
            case "Appointment" -> summarizeAppointment(snap);
            case "FinanceReceivable" -> summarizeFinanceReceivable(snap);
            case "TreatmentPlan" -> summarizeTreatmentPlan(snap);
            case "TreatmentProcedure" -> summarizeTreatmentProcedure(snap);
            case "Clinic" -> str(snap.get("nomeFantasia"));
            case "Document" -> str(snap.get("fileName"));
            case "User" -> firstNonNull(str(snap.get("name")), str(snap.get("email")));
            default -> null;
        };
    }

    private static JsonNode pickSnapshot(JsonNode root) {
        JsonNode after = root.get("after");
        if (after != null && after.isObject()) return after;
        JsonNode before = root.get("before");
        if (before != null && before.isObject()) return before;
        return null;
    }

    private static String summarizeAppointment(JsonNode snap) {
        String patient = str(snap.get("patientName"));
        String date = formatBackDate(str(snap.get("date")));
        String time = str(snap.get("time"));
        if (patient != null && date != null && time != null) return patient + " — " + date + " às " + time;
        if (patient != null && date != null) return patient + " — " + date;
        return firstNonNull(patient, str(snap.get("procedure")));
    }

    private static String summarizeFinanceReceivable(JsonNode snap) {
        String description = str(snap.get("description"));
        String patient = str(snap.get("patientName"));
        String value = brl(snap.get("value"));
        StringBuilder out = new StringBuilder();
        appendPart(out, description);
        appendPart(out, patient);
        appendPart(out, value);
        return out.length() == 0 ? null : out.toString();
    }

    private static String summarizeTreatmentPlan(JsonNode snap) {
        String title = str(snap.get("title"));
        String patient = str(snap.get("patientName"));
        if (title != null && patient != null) return title + " (" + patient + ")";
        return firstNonNull(title, patient);
    }

    private static String summarizeTreatmentProcedure(JsonNode snap) {
        String name = str(snap.get("name"));
        String tooth = str(snap.get("tooth"));
        if (name != null && tooth != null) return name + " — dente " + tooth;
        return name;
    }

    private static void appendPart(StringBuilder out, String part) {
        if (part == null || part.isEmpty()) return;
        if (out.length() > 0) out.append(" — ");
        out.append(part);
    }

    private static String str(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String text = node.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private static String brl(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (!node.isNumber() && !node.isTextual()) return null;
        try {
            BigDecimal amount = node.isNumber()
                    ? node.decimalValue()
                    : new BigDecimal(node.asText());
            return NumberFormat.getCurrencyInstance(PT_BR).format(amount);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Converte "YYYY-MM-DD..." em "DD/MM/YYYY" preservando texto se formato diferente. */
    private static String formatBackDate(String value) {
        if (value == null) return null;
        if (value.length() >= 10
                && value.charAt(4) == '-' && value.charAt(7) == '-') {
            String y = value.substring(0, 4);
            String m = value.substring(5, 7);
            String d = value.substring(8, 10);
            return d + "/" + m + "/" + y;
        }
        return value;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
