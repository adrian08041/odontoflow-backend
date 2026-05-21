package com.odontoflow.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record PendingReminderResponse(
        UUID appointmentId,
        UUID patientId,
        String patientName,
        String patientPhone,
        LocalDate date,
        String time,
        String dentistName,
        String procedure
) {}
