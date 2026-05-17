package com.odontoflow.dto.response;

import com.odontoflow.entity.Clinic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoursResponse {
    private List<ClinicHourResponse> dias;
    private String duracaoConsulta;
    private String intervalo;

    public static HoursResponse from(Clinic c) {
        return new HoursResponse(
                c.getHours().stream().map(ClinicHourResponse::from).toList(),
                c.getDuracaoConsulta(),
                c.getIntervalo()
        );
    }
}
