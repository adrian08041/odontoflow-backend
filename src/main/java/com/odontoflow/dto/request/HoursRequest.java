package com.odontoflow.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HoursRequest {
    private List<ClinicHourRequest> dias;
    private String duracaoConsulta;
    private String intervalo;
}
