package com.odontoflow.dto.request;

import com.odontoflow.entity.enums.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {
    private UUID patientId;
    private UUID dentistId;
    private LocalDate date;
    private String time;
    private Integer duration;
    private AppointmentType type;
    private String procedure;
    private String observations;
    private String patientSince;
    private String status;
}
