package com.odontoflow.dto.response;

import com.odontoflow.entity.ClinicHour;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClinicHourResponse {
    private String label;
    private boolean active;
    private String start;
    private String end;

    public static ClinicHourResponse from(ClinicHour h) {
        return new ClinicHourResponse(
                h.getLabel(),
                h.isActive(),
                h.getStart(),
                h.getEnd()
        );
    }
}
