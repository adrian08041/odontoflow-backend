package com.odontoflow.service;

import com.odontoflow.dto.response.AvailabilitySlotResponse;
import com.odontoflow.entity.Appointment;
import com.odontoflow.entity.Clinic;
import com.odontoflow.entity.ClinicHour;
import com.odontoflow.entity.Dentist;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.AppointmentRepository;
import com.odontoflow.repository.ClinicRepository;
import com.odontoflow.repository.DentistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final int MAX_WINDOW_DAYS = 7;
    private static final Pattern MINUTES = Pattern.compile("(\\d+)\\s*min");

    private final ClinicRepository clinicRepository;
    private final DentistRepository dentistRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> findAvailability(
            LocalDate from, LocalDate to, UUID dentistId, Integer durationMinOverride) {

        if (from == null || to == null) {
            throw new BusinessException("Parâmetros 'from' e 'to' obrigatórios");
        }
        if (to.isBefore(from)) {
            throw new BusinessException("'to' deve ser >= 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_WINDOW_DAYS) {
            throw new BusinessException("Janela máxima é de " + MAX_WINDOW_DAYS + " dias");
        }

        Clinic clinic = clinicRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Clínica não inicializada"));

        int slotDurationMin = durationMinOverride != null
                ? durationMinOverride
                : parseMinutes(clinic.getDuracaoConsulta());
        int intervalMin = parseMinutes(clinic.getIntervalo());
        int stride = slotDurationMin + intervalMin;

        Map<DayOfWeek, ClinicHour> hoursByDay = mapHoursByDay(clinic);

        List<Dentist> dentists = (dentistId != null)
                ? List.of(dentistRepository.findActiveById(dentistId)
                        .orElseThrow(() -> new BusinessException("Dentista não encontrado")))
                : dentistRepository.findAllActive();

        List<AvailabilitySlotResponse> result = new ArrayList<>();
        for (Dentist d : dentists) {
            List<AvailabilitySlotResponse.Slot> slots = new ArrayList<>();
            for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
                ClinicHour h = hoursByDay.get(day.getDayOfWeek());
                if (h == null || !h.isActive() || h.getStart() == null || h.getEnd() == null) {
                    continue;
                }
                Set<String> occupied = appointmentRepository
                        .findActiveByDateAndDentist(day, d.getId())
                        .stream()
                        .filter(a -> !"Cancelado".equals(a.getStatus()))
                        .map(Appointment::getTime)
                        .collect(Collectors.toCollection(HashSet::new));

                LocalTime cursor = LocalTime.parse(h.getStart());
                LocalTime end = LocalTime.parse(h.getEnd());
                while (!cursor.plusMinutes(slotDurationMin).isAfter(end)) {
                    String hhmm = cursor.toString().substring(0, 5); // garante HH:mm
                    if (!occupied.contains(hhmm)) {
                        slots.add(new AvailabilitySlotResponse.Slot(day, hhmm));
                    }
                    cursor = cursor.plusMinutes(stride);
                }
            }
            result.add(new AvailabilitySlotResponse(d.getId(), d.getName(), slots));
        }
        return result;
    }

    /** Tenta achar slot ocupado pra validar antes de criar appointment. */
    @Transactional(readOnly = true)
    public boolean isSlotFree(UUID dentistId, LocalDate date, String time) {
        return appointmentRepository.findActiveByDateAndDentist(date, dentistId).stream()
                .filter(a -> !"Cancelado".equals(a.getStatus()))
                .noneMatch(a -> time.equals(a.getTime()));
    }

    private int parseMinutes(String raw) {
        if (raw == null) return 30;
        Matcher m = MINUTES.matcher(raw);
        if (m.find()) return Integer.parseInt(m.group(1));
        try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { return 30; }
    }

    /**
     * Mapeia ClinicHour.label/position para DayOfWeek. A clínica usa rótulos em PT-BR;
     * inferimos por label primeiro (case-insensitive) com fallback por position.
     */
    private Map<DayOfWeek, ClinicHour> mapHoursByDay(Clinic clinic) {
        Map<String, DayOfWeek> aliases = Map.of(
                "segunda", DayOfWeek.MONDAY,
                "terca",   DayOfWeek.TUESDAY,
                "terça",   DayOfWeek.TUESDAY,
                "quarta",  DayOfWeek.WEDNESDAY,
                "quinta",  DayOfWeek.THURSDAY,
                "sexta",   DayOfWeek.FRIDAY,
                "sabado",  DayOfWeek.SATURDAY,
                "sábado",  DayOfWeek.SATURDAY,
                "domingo", DayOfWeek.SUNDAY
        );
        Map<DayOfWeek, ClinicHour> out = new java.util.EnumMap<>(DayOfWeek.class);
        for (ClinicHour h : clinic.getHours()) {
            DayOfWeek dow = aliases.entrySet().stream()
                    .filter(e -> h.getLabel().toLowerCase().contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            if (dow != null) out.put(dow, h);
        }
        return out;
    }
}
