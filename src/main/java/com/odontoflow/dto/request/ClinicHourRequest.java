package com.odontoflow.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicHourRequest {
    private String label;
    private Boolean active;
    private String start;
    private String end;
}
