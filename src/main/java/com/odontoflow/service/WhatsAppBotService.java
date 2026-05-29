package com.odontoflow.service;

import com.odontoflow.dto.request.BotAppointmentRequest;
import com.odontoflow.dto.request.BotRegisterPatientRequest;
import com.odontoflow.dto.request.BotRescheduleRequest;
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
import com.odontoflow.util.CpfUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.MonthDay;
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
    private final PatientService patientService;

    @Value("${app.frontend-url:https://odontoflow.up.railway.app}")
    private String landingUrl;

    /**
     * Cadastra um lead novo a partir do WhatsApp (nome + CPF; phone vem do contexto).
     * CPF inválido → 400; CPF já existente → 409 (bot faz handoff). Reusa PatientService.save
     * (normaliza telefone + audita CREATE).
     */
    @Transactional
    public Map<String, Object> registerPatient(BotRegisterPatientRequest req) {
        String digits = CpfUtil.normalize(req.cpf());
        if (!CpfUtil.isValid(digits)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF inválido");
        }
        // Pre-check load-bearing: roda ANTES do save (que lançaria BusinessException→400).
        // Compara por dígitos e inclui soft-deleted → qualquer CPF já existente vira 409 (handoff).
        if (!patientRepository.findByCpfDigits(digits).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
        }

        Patient patient = new Patient();
        patient.setName(req.name().trim());
        patient.setCpf(CpfUtil.format(digits));   // grava no formato canônico (com máscara)
        patient.setPhone(req.phone());
        patient.setStatus("Ativo");

        try {
            Patient saved = patientService.save(patient);
            return Map.of("patientId", saved.getId(), "name", saved.getName());
        } catch (DataIntegrityViolationException e) {
            // Rede de segurança p/ corrida concorrente (unique constraint do CPF) → 409, não 500.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
        }
    }

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
        // Resolve paciente por telefone e dentista por nome — o LLM alucina UUIDs opacos,
        // mas o phone é injetado fixo pelo n8n e o nome do dentista vem do get_availability.
        Patient patient = resolvePatientByPhone(req.phone())
                .orElseThrow(() -> new BusinessException("Paciente não encontrado para este telefone"));
        Dentist dentist = resolveDentistByName(req.dentistName())
                .orElseThrow(() -> new BusinessException("Dentista não encontrado: " + req.dentistName()));

        // Idempotência ANTES de resolver a data: se o MESMO paciente já tem consulta ativa com este
        // dentista no mesmo (dia/mês)+horário, devolve ela. Cobre o double tool-call do AI Agent (n8n) —
        // PRECISA vir antes do resolveSlotDate, que (via findAvailability) veria a consulta da 1ª chamada
        // como slot ocupado e lançaria "horário não disponível" na 2ª. Compara por MonthDay (o LLM alucina
        // o ANO). Slot ocupado por OUTRO paciente continua caindo no isSlotFree abaixo.
        Optional<Appointment> duplicate = appointmentRepository
                .findUpcomingActiveByPatient(patient.getId(), LocalDate.now()).stream()
                .filter(a -> a.getDentist() != null && dentist.getId().equals(a.getDentist().getId()))
                .filter(a -> req.time().equals(a.getTime()))
                .filter(a -> a.getDate() != null && req.date() != null
                          && MonthDay.from(a.getDate()).equals(MonthDay.from(req.date())))
                .findFirst();
        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        // O AI Agent (LLM) copia certo dentista/dia/mês/hora mas alucina o ANO (manda 2024).
        // Não confiamos no ano informado: resolvemos a data real do slot a partir da
        // disponibilidade do dentista (fonte de verdade), casando dia+mês+hora. Isso também
        // garante que só marcamos em slot legítimo (dia de atendimento + dentro da grade + livre).
        LocalDate date = resolveSlotDate(dentist.getId(), req.date(), req.time());

        if (!availabilityService.isSlotFree(dentist.getId(), date, req.time())) {
            throw new BusinessException("Slot já ocupado");
        }

        Clinic clinic = clinicRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Clínica não inicializada"));
        int duration = parseMinutesOr30(clinic.getDuracaoConsulta());

        Appointment a = new Appointment();
        a.setPatient(patient);
        a.setPatientName(patient.getName());
        a.setDentist(dentist);
        a.setDate(date);
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
        changes.put("date", date.toString());
        changes.put("time", req.time());
        changes.put("type", req.type().name());
        auditLogService.logAs(null, SYSTEM_USER_NAME,
                "Appointment", saved.getId(), AuditAction.CREATE, changes);

        return saved;
    }

    /** Janela máxima (dias) pra reconciliar o ano alucinado pelo LLM contra a disponibilidade real. */
    private static final int SLOT_LOOKAHEAD_DAYS = 120;

    /**
     * Resolve a data REAL do slot. O AI Agent copia certo dia/mês/hora mas alucina o ANO
     * (ex: manda 2024-06-13 para o slot real 2026-06-13). Em vez de confiar no ano informado,
     * casamos (dia, mês, hora) contra a disponibilidade do dentista e usamos a data verdadeira —
     * o ano passa a vir do backend, não do LLM. Também rejeita dia sem atendimento / fora da grade.
     */
    private LocalDate resolveSlotDate(UUID dentistId, LocalDate requested, String time) {
        if (requested == null) {
            throw new BusinessException("Data não informada.");
        }
        LocalDate today = LocalDate.now();
        // Caminho feliz: a data informada já é futura e o slot é oferecido — usa direto.
        if (!requested.isBefore(today) && isSlotOffered(dentistId, requested, time)) {
            return requested;
        }
        // Ano provavelmente alucinado: procura o próximo dia futuro com mesmo dia/mês e slot livre.
        MonthDay target = MonthDay.from(requested);
        for (int i = 0; i <= SLOT_LOOKAHEAD_DAYS; i++) {
            LocalDate candidate = today.plusDays(i);
            if (MonthDay.from(candidate).equals(target) && isSlotOffered(dentistId, candidate, time)) {
                return candidate;
            }
        }
        throw new BusinessException("Esse horário (" + time + ") não está disponível. "
                + "Use um dos horários retornados por get_availability.");
    }

    /** True se (date, time) é um slot REAL do dentista: dia de atendimento, dentro da grade e livre. */
    private boolean isSlotOffered(UUID dentistId, LocalDate date, String time) {
        return availabilityService.findAvailability(date, date, dentistId, null).stream()
                .flatMap(r -> r.slots().stream())
                .anyMatch(s -> time.equals(s.time()));
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

    /**
     * Cancela a PRÓXIMA consulta ativa do paciente, resolvida por telefone. O AI Agent só conhece
     * a próxima consulta (nextAppointment do contexto) e alucina UUIDs opacos — então operamos por
     * telefone em vez de exigir o appointmentId transcrito, mesmo motivo do create por phone/nome.
     */
    @Transactional
    public void cancelNextByPhone(String phone) {
        cancelAppointment(resolveNextActiveByPhone(phone).getId());
    }

    /** Pede remarcação da próxima consulta ativa do paciente, resolvida por telefone. */
    @Transactional
    public void requestRescheduleByPhone(String phone) {
        requestReschedule(resolveNextActiveByPhone(phone).getId());
    }

    /**
     * Remarcação ativa: move a PRÓXIMA consulta ativa do paciente (resolvida por telefone) para um
     * novo slot escolhido na conversa. Espelha o createAppointment, mas atualiza o MESMO registro
     * (preserva appointmentId/type/procedure). Trocar de dentista é permitido. Status volta a
     * "Pendente" e reminderSentAt é resetado para a consulta voltar ao ciclo de lembrete D-1.
     */
    @Transactional
    public Appointment rescheduleNextByPhone(BotRescheduleRequest req) {
        Appointment atual = resolveNextActiveByPhone(req.phone());
        Dentist dentist = resolveDentistByName(req.dentistName())
                .orElseThrow(() -> new BusinessException("Dentista não encontrado: " + req.dentistName()));

        // No-op idempotente ANTES de resolver a data: cobre o double tool-call do AI Agent e a
        // reconfirmação do mesmo slot. Precisa vir antes do resolveSlotDate porque este, via
        // findAvailability, enxerga a PRÓPRIA consulta como slot ocupado e rejeitaria o slot atual
        // ("horário não disponível"). Compara por MonthDay (ignora o ano alucinado pelo LLM) + hora + dentista.
        if (isSameSlot(atual, dentist.getId(), req.date(), req.time())) {
            return atual;
        }

        // O LLM alucina o ANO — resolvemos a data real do slot pela disponibilidade (fonte de verdade).
        LocalDate date = resolveSlotDate(dentist.getId(), req.date(), req.time());

        // Slot de destino livre, ignorando a própria consulta (que ainda ocupa o slot antigo).
        if (!availabilityService.isSlotFreeExcluding(dentist.getId(), date, req.time(), atual.getId())) {
            throw new BusinessException("Slot já ocupado");
        }

        String fromDentist = atual.getDentist() != null ? atual.getDentist().getName() : null;
        LocalDate fromDate = atual.getDate();
        String fromTime = atual.getTime();
        String fromStatus = atual.getStatus();

        atual.setDentist(dentist);
        atual.setDate(date);
        atual.setTime(req.time());
        atual.setStatus("Pendente");
        atual.setReminderSentAt(null); // volta ao ciclo de lembrete D-1 da nova data

        Appointment saved = appointmentRepository.save(atual);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("source", "Bot WhatsApp");
        changes.put("dentistName", fromDentist + " -> " + dentist.getName());
        changes.put("date", fromDate + " -> " + date);
        changes.put("time", fromTime + " -> " + req.time());
        changes.put("status", fromStatus + " -> Pendente");
        auditLogService.logAs(null, SYSTEM_USER_NAME,
                "Appointment", saved.getId(), AuditAction.UPDATE, changes);

        return saved;
    }

    /**
     * True se a consulta {@code a} já está exatamente neste dentista + (dia/mês) + horário e não-Cancelada.
     * Compara por {@link MonthDay} (ignora o ANO) porque o LLM alucina o ano da data no request — a data
     * crua não é confiável e a data resolvida não pode ser usada aqui (resolveSlotDate rejeitaria o
     * slot próprio como ocupado). Usado para detectar no-op/double-call antes de resolver a data.
     */
    private boolean isSameSlot(Appointment a, UUID dentistId, LocalDate requestedDate, String time) {
        return a.getDentist() != null
                && dentistId.equals(a.getDentist().getId())
                && a.getTime() != null && time.equals(a.getTime())
                && a.getDate() != null && requestedDate != null
                && MonthDay.from(a.getDate()).equals(MonthDay.from(requestedDate))
                && !"Cancelado".equals(a.getStatus());
    }

    private Appointment resolveNextActiveByPhone(String phone) {
        Patient patient = resolvePatientByPhone(phone)
                .orElseThrow(() -> new BusinessException("Paciente não encontrado para este telefone"));
        return appointmentRepository
                .findUpcomingActiveByPatient(patient.getId(), LocalDate.now())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException("Não há consulta futura para este paciente."));
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
