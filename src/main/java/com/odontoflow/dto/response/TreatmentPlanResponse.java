package com.odontoflow.dto.response;

import com.odontoflow.entity.TreatmentPlan;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentPlanResponse {
    private UUID id;
    private UUID patientId;
    private String patientName;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private BigDecimal total;
    private int completed;
    private int totalProcedures;
    private LocalDateTime createdAt;
    private List<TreatmentProcedureResponse> procedures;

    public static TreatmentPlanResponse from(TreatmentPlan p) {
        return new TreatmentPlanResponse(
                p.getId(),
                p.getPatient() != null ? p.getPatient().getId() : null,
                p.getPatientName(),
                p.getTitle(),
                p.getStartDate(),
                p.getEndDate(),
                p.getNotes(),
                p.getTotal(),
                p.getCompleted(),
                p.getTotalProcedures(),
                p.getCreatedAt(),
                p.getProcedures().stream().map(TreatmentProcedureResponse::from).toList()
        );
    }
}
