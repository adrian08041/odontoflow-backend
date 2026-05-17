package com.odontoflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long appointmentsToday;
    private long confirmed;
    private BigDecimal revenue;
    private long newPatients;
    private Map<String, BigDecimal> growthPercentages;
}
