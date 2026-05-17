package com.odontoflow.service;

import com.odontoflow.dto.response.DashboardAlertResponse;
import com.odontoflow.dto.response.DashboardAppointment;
import com.odontoflow.dto.response.DashboardGoalsResponse;
import com.odontoflow.dto.response.DashboardSummaryResponse;
import com.odontoflow.dto.response.WeeklyChartResponse;
import com.odontoflow.entity.Appointment;
import com.odontoflow.entity.Patient;
import com.odontoflow.repository.AppointmentRepository;
import com.odontoflow.repository.FinanceReceivableRepository;
import com.odontoflow.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final BigDecimal DEFAULT_REVENUE_GOAL = new BigDecimal("50000");
    private static final long DEFAULT_TREATMENT_GOAL = 80;

    private final AppointmentRepository appointmentRepository;
    private final FinanceReceivableRepository financeRepository;
    private final PatientRepository patientRepository;
    private final FinanceService financeService;

    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        long appointmentsToday = appointmentRepository.countByDate(today);
        long confirmed = appointmentRepository.countByDateAndStatus(today, "Confirmado");

        BigDecimal revenue = financeRepository.sumRevenueBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        BigDecimal previousRevenue = financeRepository.sumRevenueBetween(previousMonth.atDay(1), previousMonth.atEndOfMonth());

        long newPatients = patientRepository.countCreatedBetween(
                currentMonth.atDay(1).atStartOfDay(),
                currentMonth.plusMonths(1).atDay(1).atStartOfDay()
        );
        long previousNewPatients = patientRepository.countCreatedBetween(
                previousMonth.atDay(1).atStartOfDay(),
                previousMonth.plusMonths(1).atDay(1).atStartOfDay()
        );

        Map<String, BigDecimal> growth = new HashMap<>();
        growth.put("revenue", percentGrowth(revenue, previousRevenue));
        growth.put("newPatients", percentGrowth(BigDecimal.valueOf(newPatients), BigDecimal.valueOf(previousNewPatients)));

        return new DashboardSummaryResponse(appointmentsToday, confirmed, revenue, newPatients, growth);
    }

    public List<DashboardAppointment> getTodayAgenda() {
        List<Appointment> appointments = appointmentRepository.findByDate(LocalDate.now());
        List<DashboardAppointment> result = new ArrayList<>(appointments.size());
        for (Appointment a : appointments) {
            result.add(new DashboardAppointment(
                    a.getId(),
                    a.getPatientName(),
                    a.getDentist() != null ? a.getDentist().getName() : null,
                    a.getDate(),
                    a.getTime(),
                    a.getDuration(),
                    a.getProcedure(),
                    a.getStatus()
            ));
        }
        return result;
    }

    public DashboardGoalsResponse getGoals() {
        financeService.markOverdue();
        YearMonth currentMonth = YearMonth.from(LocalDate.now());
        BigDecimal revenue = financeRepository.sumRevenueBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        long completedAppointments = financeRepository.countPaidBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth());

        return new DashboardGoalsResponse(DEFAULT_REVENUE_GOAL, revenue, DEFAULT_TREATMENT_GOAL, completedAppointments);
    }

    public List<DashboardAlertResponse> getAlerts() {
        financeService.markOverdue();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        List<DashboardAlertResponse> alerts = new ArrayList<>();

        long unconfirmed = appointmentRepository.countUnconfirmedOn(tomorrow);
        if (unconfirmed > 0) {
            alerts.add(new DashboardAlertResponse(
                    "alert-unconfirmed",
                    "unconfirmed_patients",
                    "warning",
                    unconfirmed + (unconfirmed == 1 ? " paciente não confirmou amanhã" : " pacientes não confirmaram amanhã"),
                    unconfirmed,
                    "/agenda"
            ));
        }

        long overdueCount = financeRepository.countOverdue();
        if (overdueCount > 0) {
            alerts.add(new DashboardAlertResponse(
                    "alert-overdue",
                    "overdue_payments",
                    "danger",
                    overdueCount + (overdueCount == 1 ? " pagamento vencido" : " pagamentos vencidos"),
                    overdueCount,
                    "/financeiro"
            ));
        }

        List<Patient> birthdays = patientRepository.findBirthdaysOn(today.getMonthValue(), today.getDayOfMonth());
        for (Patient p : birthdays) {
            alerts.add(new DashboardAlertResponse(
                    "alert-birthday-" + p.getId(),
                    "birthday",
                    "success",
                    "Aniversariante: " + p.getName() + " 🎂",
                    1,
                    "/pacientes/" + p.getId()
            ));
        }

        long pendingReturns = appointmentRepository.countPendingReturnsFrom(today);
        if (pendingReturns > 0) {
            alerts.add(new DashboardAlertResponse(
                    "alert-returns",
                    "pending_returns",
                    "info",
                    pendingReturns + (pendingReturns == 1 ? " retorno pendente de confirmação" : " retornos pendentes de confirmação"),
                    pendingReturns,
                    "/agenda"
            ));
        }

        return alerts;
    }

    public WeeklyChartResponse getWeeklyChart() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        Map<LocalDate, Long> counts = new HashMap<>();
        for (Object[] row : appointmentRepository.countByDateBetween(monday, sunday)) {
            counts.put((LocalDate) row[0], ((Number) row[1]).longValue());
        }

        List<String> labels = new ArrayList<>(7);
        List<Long> data = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            labels.add(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", ""));
            data.add(counts.getOrDefault(day, 0L));
        }

        return new WeeklyChartResponse(labels, data);
    }

    private BigDecimal percentGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return current != null && current.signum() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }
}
