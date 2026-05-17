package com.odontoflow.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentPlanRequest {
    private UUID patientId;
    private String patient;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private List<TreatmentProcedureRequest> procedures = new ArrayList<>();
}
