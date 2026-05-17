package com.odontoflow.dto.request;

import com.odontoflow.entity.enums.FinanceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinanceStatusUpdateRequest {
    private FinanceStatus status;
}
