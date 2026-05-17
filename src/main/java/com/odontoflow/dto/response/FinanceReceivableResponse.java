package com.odontoflow.dto.response;

import com.odontoflow.entity.FinanceReceivable;
import com.odontoflow.entity.enums.FinanceStatus;
import com.odontoflow.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinanceReceivableResponse {
    private UUID id;
    private UUID patientId;
    private String patientName;
    private String description;
    private BigDecimal value;
    private LocalDate due;
    private FinanceStatus status;
    private TransactionType type;
    private String method;
    private String category;
    private String installments;
    private String notes;
    private LocalDateTime createdAt;

    public static FinanceReceivableResponse from(FinanceReceivable r) {
        return new FinanceReceivableResponse(
                r.getId(),
                r.getPatient() != null ? r.getPatient().getId() : null,
                r.getPatientName(),
                r.getDescription(),
                r.getValue(),
                r.getDue(),
                r.getStatus(),
                r.getType(),
                r.getMethod(),
                r.getCategory(),
                r.getInstallments(),
                r.getNotes(),
                r.getCreatedAt()
        );
    }
}
