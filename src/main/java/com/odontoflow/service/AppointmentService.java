package com.odontoflow.service;

import com.odontoflow.dto.request.AppointmentRequest;
import com.odontoflow.dto.request.RescheduleRequest;
import com.odontoflow.dto.request.StatusUpdateRequest;
import com.odontoflow.entity.Appointment;
import com.odontoflow.entity.Dentist;
import com.odontoflow.entity.Patient;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientService patientService;
    private final DentistService dentistService;

    public List<Appointment> findAll(LocalDate startDate, LocalDate endDate, UUID dentistId) {
        return appointmentRepository.findAllFiltered(startDate, endDate, dentistId);
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

        return appointmentRepository.save(appointment);
    }

    public Appointment update(UUID id, AppointmentRequest request) {
        Appointment existing = findById(id);
        Patient patient = patientService.findById(request.getPatientId());
        Dentist dentist = dentistService.findById(request.getDentistId());

        validateDuration(request.getDuration());

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

        return appointmentRepository.save(existing);
    }

    public Appointment reschedule(UUID id, RescheduleRequest request) {
        Appointment appointment = findById(id);
        appointment.setDate(request.getDate());
        appointment.setTime(request.getTime());
        return appointmentRepository.save(appointment);
    }

    public Appointment updateStatus(UUID id, StatusUpdateRequest request) {
        Appointment appointment = findById(id);
        appointment.setStatus(request.getStatus());
        return appointmentRepository.save(appointment);
    }

    public void delete(UUID id) {
        Appointment appointment = findById(id);
        appointment.setDeletedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    private void validateDuration(Integer duration) {
        if (duration == null || duration < 15) {
            throw new BusinessException("A duração mínima do agendamento é 15 minutos.");
        }
    }
}
