package com.odontoflow.dto.request;

import com.odontoflow.entity.enums.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

public record BotAppointmentRequest(
        @NotNull UUID patientId,
        @NotNull UUID dentistId,
        @NotNull LocalDate date,
        @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
                           message = "time deve estar em HH:mm") String time,
        @NotNull AppointmentType type,
        String procedure,
        String notes
) {}
