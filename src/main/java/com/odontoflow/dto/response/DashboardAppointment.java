package com.odontoflow.dto.response;

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
public class DashboardAppointment {
    private UUID id;
    private String patientName;
    private String dentistName;
    private LocalDate date;
    private String time;
    private Integer duration;
    private String procedure;
    private String status;
}
