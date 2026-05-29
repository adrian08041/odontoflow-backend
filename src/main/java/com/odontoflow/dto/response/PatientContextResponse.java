package com.odontoflow.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientContextResponse(
        UUID patientId,
        String name,
        NextAppointment nextAppointment,
        boolean hasOverdueReceivables,
        boolean treatmentInProgress,
        ClinicInfo clinic
) {
    public record NextAppointment(
            UUID appointmentId,
            LocalDate date,
            String time,
            String dentistName,
            String procedure,
            String status
    ) {}

    public record ClinicInfo(
            String name,
            String phone,
            String address,
            List<Hour> hours,
            List<String> insurances,
            String duracaoConsulta,
            String intervalo,
            String landingUrl
    ) {
        public record Hour(String day, boolean active, String start, String end) {}
    }
}
