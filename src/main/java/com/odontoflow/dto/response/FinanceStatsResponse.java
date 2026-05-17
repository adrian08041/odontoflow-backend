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
public class FinanceStatsResponse {
    private BigDecimal revenue;
    private BigDecimal toReceive;
    private BigDecimal overdue;
    private BigDecimal avgTicket;
    private BigDecimal revenueGrowth;
}
