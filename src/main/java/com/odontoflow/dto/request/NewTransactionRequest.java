package com.odontoflow.dto.request;

import com.odontoflow.entity.enums.FinanceStatus;
import com.odontoflow.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewTransactionRequest {
    private TransactionType type;
    private UUID patientId;
    private String patient;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String method;
    private String category;
    private String installments;
    private String notes;
    private FinanceStatus status;
}
