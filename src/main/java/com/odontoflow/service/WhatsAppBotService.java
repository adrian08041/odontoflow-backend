package com.odontoflow.service;

import com.odontoflow.dto.request.BotAppointmentRequest;
import com.odontoflow.dto.request.HandoffRequest;
import com.odontoflow.dto.response.HandoffResponse;
import com.odontoflow.dto.response.PatientContextResponse;
import com.odontoflow.entity.Appointment;
import com.odontoflow.entity.Clinic;
import com.odontoflow.entity.ClinicHour;
import com.odontoflow.entity.Dentist;
import com.odontoflow.entity.Patient;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.entity.enums.ReminderAction;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.AppointmentRepository;
import com.odontoflow.repository.ClinicRepository;
import com.odontoflow.repository.DentistRepository;
import com.odontoflow.repository.FinanceReceivableRepository;
import com.odontoflow.repository.PatientRepository;
import com.odontoflow.repository.TreatmentPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WhatsAppBotService {

    private static final String SYSTEM_USER_NAME = "Bot WhatsApp";
    private static final String ADMIN_WHATSAPP_FALLBACK = "5511915935231";

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final DentistRepository dentistRepository;
    private final ClinicRepository clinicRepository;
    private final FinanceReceivableRepository financeRepository;
    private final TreatmentPlanRepository treatmentRepository;
    private final AvailabilityService availabilityService;
    private final WhatsAppReminderService reminderService;
    private final AuditLogService auditLogService;

    @Value("${app.frontend-url:https://odontoflow.up.railway.app}")
    private String landingUrl;

    @Value("${app.whatsapp.admin-number:" + ADMIN_WHATSAPP_FALLBACK + "}")
    private String adminPhone;

    /** Dossiê filtrado por LGPD (sem CPF/email/endereço/valores). */
    @Transactional(readOnly = true)
    public PatientContextResponse getPatientContext(String phone) {
        Optional<Patient> opt = resolvePatientByPhone(phone);

        Clinic clinic = clinicRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Clínica não inicializada"));
        PatientContextResponse.ClinicInfo clinicInfo = buildClinicInfo(clinic);

        if (opt.isEmpty()) {
            return new PatientContextResponse(null, null, null, false, false, clinicInfo);
        }
        Patient p = opt.get();

        Optional<Appointment> next = appointmentRepository
                .findUpcomingActiveByPatient(p.getId(), LocalDate.now())
                .stream()
                .findFirst();

        PatientContextResponse.NextAppointment nextDto = next
                .map(a -> new PatientContextResponse.NextAppointment(
                        a.getId(), a.getDate(), a.getTime(),
                        a.getDentist().getName(), a.getProcedure(), a.getStatus()))
                .orElse(null);

        // hasOverdueReceivables: simples — conta receivables Atrasado deste paciente
        boolean overdue = financeRepository.findOverdue(LocalDate.now()).stream()
                .anyMatch(r -> r.getPatient() != null
                            && p.getId().equals(r.getPatient().getId()));

        // treatmentInProgress: existe plano não concluído
        boolean treatment = treatmentRepository.findAll().stream()
                .filter(t -> t.getDeletedAt() == null)
                .filter(t -> t.getPatient() != null && p.getId().equals(t.getPatient().getId()))
                .anyMatch(t -> t.getCompletedAt() == null);

        return new PatientContextResponse(
                p.getId(), p.getName(), nextDto, overdue, treatment, clinicInfo);
    }
    /**
     * Resolve paciente ativo pelo telefone, comparando só os dígitos (tolera máscara/formato
     * e a normalização do uazapi). O(N) sobre pacientes ativos — aceitável pra PI; em prod,
     * trocar por query dedicada `findActiveByPhone(String)` no PatientRepository.
     */
    private Optional<Patient> resolvePatientByPhone(String phone) {
        String normalized = onlyDigits(phone);
        if (normalized.isEmpty()) return Optional.empty();
        return patientRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null
                          && p.getPhone() != null
                          && onlyDigits(p.getPhone()).equals(normalized))
                .findFirst();
    }

    /**
     * Resolve dentista ativo pelo nome (case-insensitive). Tolera o LLM mandar variações como
     * "Dra. Ana Souza" / "Ana Souza" / "ana souza": tenta match exato normalizado, depois
     * contains em qualquer direção.
     */
    private Optional<Dentist> resolveDentistByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        List<Dentist> dentists = dentistRepository.findAllActive();
        String target = normalizeName(name);
        Optional<Dentist> exact = dentists.stream()
                .filter(d -> normalizeName(d.getName()).equals(target))
                .findFirst();
        if (exact.isPresent()) return exact;
        return dentists.stream()
                .filter(d -> {
                    String n = normalizeName(d.getName());
                    return n.contains(target) || target.contains(n);
                })
                .findFirst();
    }

    /** Lowercase, remove pontuação, título (dr/dra) e espaços extras — pra comparar nomes. */
    private static String normalizeName(String s) {
        return s.toLowerCase()
                .replace(".", "")
                .replaceAll("\\b(dra|dr|doutora|doutor)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Transactional
    public Appointment createAppointment(BotAppointmentRequest req) {
        if (req.date().isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível agendar em data passada (" + req.date()
                    + "). Use uma das datas retornadas por get_availability.");
        }
        // Resolve paciente por telefone e dentista por nome — o LLM alucina UUIDs opacos,
        // mas o phone é injetado fixo pelo n8n e o nome do dentista vem do get_availability.
        Patient patient = resolvePatientByPhone(req.phone())
                .orElseThrow(() -> new BusinessException("Paciente não encontrado para este telefone"));
        Dentist dentist = resolveDentistByName(req.dentistName())
                .orElseThrow(() -> new BusinessException("Dentista não encontrado: " + req.dentistName()));

        // Idempotência: se o MESMO paciente já tem consulta ativa neste slot, devolve ela.
        // Cobre o double tool-call do AI Agent (n8n) — sem isso, a 2ª chamada recebia 400
        // "Slot já ocupado" apesar de a 1ª ter criado, e o bot reportava erro de uma operação
        // bem-sucedida. Slot ocupado por OUTRO paciente continua caindo no 400 abaixo.
        Optional<Appointment> duplicate = appointmentRepository
                .findActiveByDateAndDentist(req.date(), dentist.getId()).stream()
                .filter(a -> req.time().equals(a.getTime()))
                .filter(a -> !"Cancelado".equals(a.getStatus()))
                .filter(a -> a.getPatient() != null && patient.getId().equals(a.getPatient().getId()))
                .findFirst();
        if (duplicate.isPresent()) {
            return duplicate.get();
        }
        if (!availabilityService.isSlotFree(dentist.getId(), req.date(), req.time())) {
            throw new BusinessException("Slot já ocupado");
        }

        Clinic clinic = clinicRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Clínica não inicializada"));
        int duration = parseMinutesOr30(clinic.getDuracaoConsulta());

        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setPatientName(patient.getName());
        a.setDentist(dentist);
        a.setDate(req.date());
        a.setTime(req.time());
        a.setDuration(duration);
        a.setType(req.type());
        a.setProcedure(req.procedure() != null ? req.procedure() : req.type().name());
        a.setObservations(req.notes());
        a.setStatus("Pendente");

        Appointment saved = appointmentRepository.save(a);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("source", "Bot WhatsApp");
        changes.put("patientName", patient.getName());
        changes.put("dentistName", dentist.getName());
        changes.put("date", req.date().toString());
        changes.put("time", req.time());
        changes.put("type", req.type().name());
        auditLogService.logAs(null, SYSTEM_USER_NAME,
                "Appointment", saved.getId(), AuditAction.CREATE, changes);

        return saved;
    }

    @Transactional
    public void cancelAppointment(UUID appointmentId) {
        // Delega pra Fase 1 — reusa whitelist ACTIVE_STATUSES + idempotência + audit
        reminderService.applyReminderResponse(
                appointmentId, ReminderAction.CANCELED, java.time.LocalDateTime.now());
    }

    @Transactional
    public void requestReschedule(UUID appointmentId) {
        reminderService.applyReminderResponse(
                appointmentId, ReminderAction.RESCHEDULE_REQUESTED, java.time.LocalDateTime.now());
    }

    @Transactional
    public HandoffResponse handoff(HandoffRequest req) {
        Map<String, Object> changes = Map.of(
                "phone", maskPhone(req.phone()),
                "reason", req.reason()
        );
        auditLogService.logAs(null, SYSTEM_USER_NAME,
                "Conversation", null, AuditAction.UPDATE, changes);

        String message = "Atendimento humano solicitado por " + maskPhone(req.phone())
                + ". Motivo: " + req.reason();
        return new HandoffResponse(adminPhone, message);
    }

    private PatientContextResponse.ClinicInfo buildClinicInfo(Clinic clinic) {
        List<PatientContextResponse.ClinicInfo.Hour> hours = clinic.getHours().stream()
                .sorted(java.util.Comparator.comparingInt(ClinicHour::getPosition))
                .map(h -> new PatientContextResponse.ClinicInfo.Hour(
                        h.getLabel(), h.isActive(), h.getStart(), h.getEnd()))
                .toList();
        // Insurance list pode vir do InsuranceRepository — opcional, deixar simples primeiro
        return new PatientContextResponse.ClinicInfo(
                clinic.getNomeFantasia(),
                clinic.getTelefone(),
                clinic.getEndereco(),
                hours,
                List.of(), // insurances — preencher em Task 6 se necessário
                clinic.getDuracaoConsulta(),
                clinic.getIntervalo(),
                landingUrl
        );
    }

    private static String onlyDigits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String maskPhone(String phone) {
        String digits = onlyDigits(phone);
        if (digits.length() < 4) return "****";
        return "*****" + digits.substring(digits.length() - 4);
    }

    private static int parseMinutesOr30(String raw) {
        if (raw == null) return 30;
        try {
            return Integer.parseInt(raw.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 30;
        }
    }
}
