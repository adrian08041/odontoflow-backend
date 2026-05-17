package com.odontoflow.dto.response;

import com.odontoflow.entity.Insurance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceResponse {
    private UUID id;
    private String name;
    private String code;
    private String type;
    private String discount;
    private String status;

    public static InsuranceResponse from(Insurance i) {
        return new InsuranceResponse(
                i.getId(),
                i.getName(),
                i.getCode(),
                i.getType(),
                i.getDiscount(),
                i.getStatus()
        );
    }
}
