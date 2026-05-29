package com.odontoflow.service;

import com.odontoflow.dto.request.AppointmentRequest;
import com.odontoflow.dto.request.RescheduleRequest;
import com.odontoflow.dto.request.StatusUpdateRequest;
import com.odontoflow.entity.Appointment;
import com.odontoflow.entity.Dentist;
import com.odontoflow.entity.Patient;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.AppointmentRepository;
import com.odontoflow.util.AuditChanges;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientService patientService;
    private final DentistService dentistService;
    private final AuditLogService auditLogService;

    public List<Appointment> findAll(LocalDate startDate, LocalDate endDate, UUID dentistId, UUID patientId) {
        return appointmentRepository.findAllFiltered(startDate, endDate, dentistId, patientId);
    }

    public Appointment findById(UUID id) {
        return appointmentRepository.findActiveById(id)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));
    }

    public Appointment create(AppointmentRequest request) {
        Patient patient = patientService.findById(request.getPatientId());
        Dentist dentist = dentistService.findById(request.getDentistId());

        validateDuration(request.getDuration());

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setPatientName(patient.getName());
        appointment.setDentist(dentist);
        appointment.setDate(request.getDate());
        appointment.setTime(request.getTime());
        appointment.setDuration(request.getDuration());
        appointment.setType(request.getType());
        appointment.setProcedure(request.getProcedure());
        appointment.setObservations(request.getObservations());
        appointment.setPatientSince(request.getPatientSince());
        if (request.getStatus() != null) {
            appointment.setStatus(request.getStatus());
        }

        Appointment saved = appointmentRepository.save(appointment);
        auditLogService.log("Appointment", saved.getId(), AuditAction.CREATE,
                AuditChanges.after(AuditChanges.snapshot(saved)));
        return saved;
    }

    public Appointment update(UUID id, AppointmentRequest request) {
        Appointment existing = findById(id);
        Patient patient = patientService.findById(request.getPatientId());
        Dentist dentist = dentistService.findById(request.getDentistId());

        validateDuration(request.getDuration());

        Map<String, Object> before = AuditChanges.snapshot(existing);

        existing.setPatient(patient);
        existing.setPatientName(patient.getName());
        existing.setDentist(dentist);
        existing.setDate(request.getDate());
        existing.setTime(request.getTime());
        existing.setDuration(request.getDuration());
        existing.setType(request.getType());
        existing.setProcedure(request.getProcedure());
        existing.setObservations(request.getObservations());
        existing.setPatientSince(request.getPatientSince());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        Appointment saved = appointmentRepository.save(existing);
        auditLogService.log("Appointment", saved.getId(), AuditAction.UPDATE,
                AuditChanges.diff(before, AuditChanges.snapshot(saved)));
        return saved;
    }

    public Appointment reschedule(UUID id, RescheduleRequest request) {
        Appointment appointment = findById(id);
        Map<String, Object> before = Map.of(
                "date", appointment.getDate().toString(),
                "time", appointment.getTime());
        appointment.setDate(request.getDate());
        appointment.setTime(request.getTime());
        Appointment saved = appointmentRepository.save(appointment);
        Map<String, Object> after = Map.of(
                "date", saved.getDate().toString(),
                "time", saved.getTime());
        auditLogService.log("Appointment", saved.getId(), AuditAction.UPDATE,
                AuditChanges.diff(before, after));
        return saved;
    }

    public Appointment updateStatus(UUID id, StatusUpdateRequest request) {
        Appointment appointment = findById(id);
        Map<String, Object> before = Map.of("status", String.valueOf(appointment.getStatus()));
        appointment.setStatus(request.getStatus());
        Appointment saved = appointmentRepository.save(appointment);
        Map<String, Object> after = Map.of("status", String.valueOf(saved.getStatus()));
        auditLogService.log("Appointment", saved.getId(), AuditAction.UPDATE,
                AuditChanges.diff(before, after));
        return saved;
    }

    public void delete(UUID id) {
        Appointment appointment = findById(id);
        Map<String, Object> before = AuditChanges.snapshot(appointment);
        appointment.setDeletedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
        auditLogService.log("Appointment", appointment.getId(), AuditAction.DELETE,
                AuditChanges.before(before));
    }

    private void validateDuration(Integer duration) {
        if (duration == null || duration < 15) {
            throw new BusinessException("A duração mínima do agendamento é 15 minutos.");
        }
    }
}
