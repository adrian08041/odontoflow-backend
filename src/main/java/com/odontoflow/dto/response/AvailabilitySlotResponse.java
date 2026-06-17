package com.odontoflow.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailabilitySlotResponse(
        UUID dentistId,
        String dentistName,
        List<Slot> slots
) {
    /**
     * {@code date} (YYYY-MM-DD) e {@code time} (HH:mm) são para o agendamento; {@code label}
     * é o texto pronto em PT-BR ("17/06 (quarta-feira)") para o bot exibir sem ter que
     * calcular o dia da semana (o LLM erra esse cálculo).
     */
    public record Slot(LocalDate date, String time, String label) {}
}
