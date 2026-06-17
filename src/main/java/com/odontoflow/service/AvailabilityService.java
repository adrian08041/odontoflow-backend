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
    private static final int GRID_MINUTES = 30;
    /** Dias da semana em PT-BR indexados por {@link DayOfWeek#getValue()} (1=segunda … 7=domingo). */
    private static final String[] WEEKDAY_PT = {
            "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira",
            "sexta-feira", "sábado", "domingo"
    };

    private final ClinicRepository clinicRepository;
    private final DentistRepository dentistRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> findAvailability(
            LocalDate from, LocalDate to, UUID dentistId, Integer durationMinOverride) {

        if (from == null || to == null) {
            throw new BusinessException("Parâmetros 'from' e 'to' obrigatórios");
        }

        // O AI Agent (LLM) às vezes chama com ano errado (2024/2025) ou com datas de mês/dia
        // já passado. Sem normalizar, o bot oferecia slots no passado ("10/06") ou saltava um
        // ano inteiro. Garantimos sempre uma janela futura próxima:
        LocalDate today = LocalDate.now();
        from = bringYearToCurrent(from, today); // ano anterior ao corrente → ano corrente
        to = bringYearToCurrent(to, today);
        if (from.isBefore(today)) {
            from = today; // nunca começa no passado (mês/dia já passou no ano corrente)
        }
        if (to.isBefore(from)) {
            to = from.plusDays(MAX_WINDOW_DAYS - 1); // janela invertida/passada → reabre padrão
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
        int stride = alignUpToGrid(Math.max(slotDurationMin + intervalMin, GRID_MINUTES));

        Map<DayOfWeek, ClinicHour> hoursByDay = mapHoursByDay(clinic);

        List<Dentist> dentists = (dentistId != null)
                ? List.of(dentistRepository.findActiveById(dentistId)
                        .orElseThrow(() -> new BusinessException("Dentista não encontrado")))
                : dentistRepository.findAllActive();

        List<AvailabilitySlotResponse> result = new ArrayList<>();
        for (Dentist d : dentists) {
            List<AvailabilitySlotResponse.Slot> slots = new ArrayList<>();
            for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
                if (day.isBefore(LocalDate.now())) {
                    continue; // nunca oferece data passada (defensivo, além do bump em from/to)
                }
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

                String dayLabel = formatDayLabel(day);
                boolean isToday = day.isEqual(today);
                LocalTime nowTime = LocalTime.now();
                LocalTime cursor = alignUpToGrid(LocalTime.parse(h.getStart()));
                LocalTime end = LocalTime.parse(h.getEnd());
                while (!cursor.plusMinutes(slotDurationMin).isAfter(end)) {
                    String hhmm = cursor.toString().substring(0, 5); // garante HH:mm
                    // No dia de hoje, descarta horários cujo início já passou (<= agora).
                    boolean alreadyPassed = isToday && !cursor.isAfter(nowTime);
                    if (!alreadyPassed && !occupied.contains(hhmm)) {
                        slots.add(new AvailabilitySlotResponse.Slot(day, hhmm, dayLabel));
                    }
                    cursor = cursor.plusMinutes(stride);
                }
            }
            result.add(new AvailabilitySlotResponse(d.getId(), d.getName(), slots));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public boolean isSlotFree(UUID dentistId, LocalDate date, String time) {
        return appointmentRepository.findActiveByDateAndDentist(date, dentistId).stream()
                .filter(a -> !"Cancelado".equals(a.getStatus()))
                .noneMatch(a -> time.equals(a.getTime()));
    }

    @Transactional(readOnly = true)
    public boolean isSlotFreeExcluding(UUID dentistId, LocalDate date, String time, UUID excludeAppointmentId) {
        return appointmentRepository.findActiveByDateAndDentist(date, dentistId).stream()
                .filter(a -> !"Cancelado".equals(a.getStatus()))
                .filter(a -> excludeAppointmentId == null || !excludeAppointmentId.equals(a.getId()))
                .noneMatch(a -> time.equals(a.getTime()));
    }


    /** Ano anterior ao corrente (LLM alucina 2024/2025) → traz para o ano corrente, preservando mês/dia. */
    private LocalDate bringYearToCurrent(LocalDate d, LocalDate today) {
        return d.getYear() < today.getYear() ? safeWithYear(d, today.getYear()) : d;
    }

    /** Texto pronto para o bot exibir: "17/06 (quarta-feira)" — evita o LLM calcular o dia da semana. */
    private static String formatDayLabel(LocalDate day) {
        return String.format("%02d/%02d (%s)",
                day.getDayOfMonth(), day.getMonthValue(),
                WEEKDAY_PT[day.getDayOfWeek().getValue() - 1]);
    }

    private LocalDate safeWithYear(LocalDate d, int year) {
        if (d.getMonthValue() == 2 && d.getDayOfMonth() == 29 && !java.time.Year.isLeap(year)) {
            return LocalDate.of(year, 2, 28);
        }
        return d.withYear(year);
    }

    private static int alignUpToGrid(int minutes) {
        return ((minutes + GRID_MINUTES - 1) / GRID_MINUTES) * GRID_MINUTES;
    }

    private static LocalTime alignUpToGrid(LocalTime t) {
        int aligned = alignUpToGrid(t.getHour() * 60 + t.getMinute());
        return aligned >= 24 * 60 ? LocalTime.MAX : LocalTime.of(aligned / 60, aligned % 60);
    }

    private int parseMinutes(String raw) {
        if (raw == null) return 30;
        Matcher m = MINUTES.matcher(raw);
        if (m.find()) return Integer.parseInt(m.group(1));
        try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { return 30; }
    }


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
