package com.odontoflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardGoalsResponse {
    private BigDecimal revenueGoal;
    private BigDecimal revenueActual;
    private long treatmentGoal;
    private long treatmentActual;
}
