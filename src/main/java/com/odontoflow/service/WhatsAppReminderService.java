package com.odontoflow.service;

import com.odontoflow.dto.response.PendingReminderResponse;
import com.odontoflow.entity.Appointment;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.entity.enums.ReminderAction;
import com.odontoflow.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WhatsAppReminderService {

    private static final String SYSTEM_USER_NAME = "Bot WhatsApp";

    /** Status que aceitam transição via bot. Demais (Cancelado, Concluído, etc.) → 409. */
    private static final Set<String> ACTIVE_STATUSES =
            Set.of("Pendente", "Confirmado", "Remarcar");

    private final AppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;

    public List<PendingReminderResponse> findPendingReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> appointments = appointmentRepository
                .findPendingReminders(tomorrow, tomorrow);

        return appointments.stream()
                .map(a -> new PendingReminderResponse(
                        a.getId(),
                        a.getPatient().getId(),
                        a.getPatientName(),
                        a.getPatient().getPhone(),
                        a.getDate(),
                        a.getTime(),
                        a.getDentist().getName(),
                        a.getProcedure()
                ))
                .toList();
    }

    @Transactional
    public void markReminderSent(UUID appointmentId) {
        Appointment a = loadActive(appointmentId);
        if (a.getReminderSentAt() != null) {
            return; // idempotente: não re-marca nem re-audita
        }
        LocalDateTime now = LocalDateTime.now();
        a.setReminderSentAt(now);

        auditLogService.logAs(null, SYSTEM_USER_NAME,
                "Appointment", appointmentId, AuditAction.UPDATE,
                Map.of("reminderSentAt", now.toString()));
    }

    @Transactional
    public void applyReminderResponse(UUID appointmentId,
                                      ReminderAction action,
                                      LocalDateTime respondedAt) {
        Appointment a = loadActive(appointmentId);

        LocalDateTime apptDateTime = LocalDateTime.of(
                a.getDate(),
                LocalTime.parse(a.getTime())
        );
        if (apptDateTime.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "Consulta já aconteceu");
        }

        String newStatus = switch (action) {
            case CONFIRMED -> "Confirmado";
            case CANCELED -> "Cancelado";
            case RESCHEDULE_REQUESTED -> "Remarcar";
        };

        String current = a.getStatus();

        // Idempotente: mesma ação aplicada ao status atual → no-op
        if (newStatus.equals(current)) {
            return;
        }

        // Whitelist de status que aceitam transição via bot
        if (!ACTIVE_STATUSES.contains(current)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Consulta em status '" + current + "' não aceita ação do bot");
        }

        a.setStatus(newStatus);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("before", Map.of("status", current));
        changes.put("after", Map.of("status", newStatus));
        changes.put("respondedAt",
                respondedAt != null ? respondedAt.toString() : LocalDateTime.now().toString());

        auditLogService.logAs(null, SYSTEM_USER_NAME,
                "Appointment", appointmentId, AuditAction.UPDATE, changes);
    }

    private Appointment loadActive(UUID id) {
        return appointmentRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment não encontrado"));
    }
}
