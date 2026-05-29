package com.odontoflow.util;

import com.odontoflow.entity.Appointment;
import com.odontoflow.entity.Clinic;
import com.odontoflow.entity.Dentist;
import com.odontoflow.entity.Document;
import com.odontoflow.entity.FinanceReceivable;
import com.odontoflow.entity.Insurance;
import com.odontoflow.entity.Patient;
import com.odontoflow.entity.TreatmentPlan;
import com.odontoflow.entity.TreatmentProcedure;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper para construir o payload `changes` dos AuditLog. Mantém snapshots compactos
 * (só campos relevantes) e fora dos services de domínio.
 *
 * <p><b>Atenção:</b> as chaves dos maps abaixo são lidas por {@link AuditSummary}
 * (back) e por {@code lib/utils/audit-helpers.ts#extractEntitySummary} (front).
 * Ao renomear/remover um campo aqui, atualizar também esses dois consumidores —
 * caso contrário a descrição contextual dos logs vira {@code null}
 * silenciosamente.
 */
public final class AuditChanges {

    private AuditChanges() {}

    public static Map<String, Object> after(Object snapshot) {
        return Map.of("after", snapshot);
    }

    public static Map<String, Object> before(Object snapshot) {
        return Map.of("before", snapshot);
    }

    public static Map<String, Object> diff(Object before, Object after) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("before", before);
        out.put("after", after);
        return out;
    }

    // ---------- snapshots por entidade ----------

    public static Map<String, Object> snapshot(Patient p) {
        return ofEntries(
                "id", p.getId(),
                "name", p.getName(),
                "cpf", p.getCpf(),
                "email", p.getEmail(),
                "phone", p.getPhone(),
                "status", p.getStatus(),
                "insurance", p.getInsurance()
        );
    }

    public static Map<String, Object> snapshot(Appointment a) {
        return ofEntries(
                "id", a.getId(),
                "patientId", a.getPatient() != null ? a.getPatient().getId() : null,
                "patientName", a.getPatientName(),
                "dentistId", a.getDentist() != null ? a.getDentist().getId() : null,
                "date", a.getDate() != null ? a.getDate().toString() : null,
                "time", a.getTime(),
                "duration", a.getDuration(),
                "type", a.getType() != null ? a.getType().name() : null,
                "status", a.getStatus(),
                "procedure", a.getProcedure()
        );
    }

    public static Map<String, Object> snapshot(FinanceReceivable f) {
        return ofEntries(
                "id", f.getId(),
                "patientName", f.getPatientName(),
                "description", f.getDescription(),
                "value", f.getValue(),
                "due", f.getDue() != null ? f.getDue().toString() : null,
                "status", f.getStatus() != null ? f.getStatus().name() : null,
                "method", f.getMethod(),
                "type", f.getType() != null ? f.getType().name() : null,
                "paidAt", f.getPaidAt() != null ? f.getPaidAt().toString() : null
        );
    }

    public static Map<String, Object> snapshot(TreatmentPlan t) {
        return ofEntries(
                "id", t.getId(),
                "title", t.getTitle(),
                "patientName", t.getPatientName(),
                "totalProcedures", t.getTotalProcedures(),
                "completed", t.getCompleted(),
                "total", t.getTotal(),
                "endDate", t.getEndDate() != null ? t.getEndDate().toString() : null,
                "completedAt", t.getCompletedAt() != null ? t.getCompletedAt().toString() : null
        );
    }

    public static Map<String, Object> snapshot(TreatmentProcedure tp) {
        return ofEntries(
                "id", tp.getId(),
                "name", tp.getName(),
                "tooth", tp.getTooth(),
                "value", tp.getValue(),
                "done", tp.isDone(),
                "paid", tp.isPaid(),
                "position", tp.getPosition()
        );
    }

    public static Map<String, Object> snapshot(Clinic c) {
        return ofEntries(
                "id", c.getId(),
                "nomeFantasia", c.getNomeFantasia(),
                "cnpj", c.getCnpj(),
                "telefone", c.getTelefone(),
                "email", c.getEmail(),
                "duracaoConsulta", c.getDuracaoConsulta(),
                "intervalo", c.getIntervalo(),
                "revenueGoal", c.getRevenueGoal(),
                "treatmentGoal", c.getTreatmentGoal()
        );
    }

    public static Map<String, Object> snapshot(Insurance i) {
        return ofEntries(
                "id", i.getId(),
                "name", i.getName(),
                "code", i.getCode(),
                "type", i.getType(),
                "discount", i.getDiscount(),
                "status", i.getStatus()
        );
    }

    public static Map<String, Object> snapshot(Dentist d) {
        return ofEntries(
                "id", d.getId(),
                "name", d.getName(),
                "specialty", d.getSpecialty()
        );
    }

    public static Map<String, Object> snapshot(Document d) {
        return ofEntries(
                "id", d.getId(),
                "patientId", d.getPatient() != null ? d.getPatient().getId() : null,
                "fileName", d.getFileName(),
                "contentType", d.getContentType(),
                "fileSizeBytes", d.getFileSizeBytes()
        );
    }

    /** Map preservando ordem de inserção e aceitando valores null. */
    private static Map<String, Object> ofEntries(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("Pares chave/valor desbalanceados");
        }
        Map<String, Object> out = new LinkedHashMap<>(kv.length / 2);
        for (int i = 0; i < kv.length; i += 2) {
            out.put((String) kv[i], kv[i + 1]);
        }
        return out;
    }
}
