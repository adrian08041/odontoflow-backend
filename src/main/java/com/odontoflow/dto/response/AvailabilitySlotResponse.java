package com.odontoflow.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailabilitySlotResponse(
        UUID dentistId,
        String dentistName,
        List<Slot> slots
) {
    public record Slot(LocalDate date, String time) {}
}
