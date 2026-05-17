package com.odontoflow.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentProcedureRequest {
    private UUID id;
    private String tooth;
    private String name;
    private BigDecimal value;
    private Boolean paid;
    private Boolean done;
}
