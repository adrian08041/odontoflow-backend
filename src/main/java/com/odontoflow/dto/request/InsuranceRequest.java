package com.odontoflow.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InsuranceRequest {
    private String name;
    private String code;
    private String type;
    private String discount;
    private String status;
}
