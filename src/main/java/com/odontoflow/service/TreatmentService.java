package com.odontoflow.service;

import com.odontoflow.dto.request.TreatmentPlanRequest;
import com.odontoflow.dto.request.TreatmentProcedureRequest;
import com.odontoflow.dto.response.TreatmentPlanResponse;
import com.odontoflow.entity.Patient;
import com.odontoflow.entity.TreatmentPlan;
import com.odontoflow.entity.TreatmentProcedure;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.TreatmentPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TreatmentService {

    private final TreatmentPlanRepository repository;
    private final PatientService patientService;

    @Transactional(readOnly = true)
    public Page<TreatmentPlanResponse> findAll(String search, Pageable pageable) {
        return repository.findAllFiltered(search, pageable).map(TreatmentPlanResponse::from);
    }

    @Transactional(readOnly = true)
    public TreatmentPlanResponse findById(UUID id) {
        return TreatmentPlanResponse.from(getOrThrow(id));
    }

    @Transactional
    public TreatmentPlanResponse create(TreatmentPlanRequest request) {
        validate(request);

        TreatmentPlan plan = new TreatmentPlan();
        bindPatient(plan, request);
        plan.setTitle(request.getTitle());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setNotes(request.getNotes());

        List<TreatmentProcedureRequest> reqProcs = request.getProcedures();
        for (int i = 0; i < reqProcs.size(); i++) {
            plan.getProcedures().add(buildProcedure(plan, reqProcs.get(i), i));
        }

        recalculateTotals(plan);
        return TreatmentPlanResponse.from(repository.saveAndFlush(plan));
    }

    @Transactional
    public TreatmentPlanResponse update(UUID id, TreatmentPlanRequest request) {
        validate(request);
        TreatmentPlan plan = getOrThrow(id);

        bindPatient(plan, request);
        plan.setTitle(request.getTitle());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setNotes(request.getNotes());

        mergeProcedures(plan, request.getProcedures());
        recalculateTotals(plan);
        return TreatmentPlanResponse.from(repository.save(plan));
    }

    private void mergeProcedures(TreatmentPlan plan, List<TreatmentProcedureRequest> reqProcs) {
        Map<UUID, TreatmentProcedure> existingById = new HashMap<>();
        for (TreatmentProcedure existing : plan.getProcedures()) {
            if (existing.getId() != null) {
                existingById.put(existing.getId(), existing);
            }
        }
        Set<UUID> keepIds = new HashSet<>();

        for (int i = 0; i < reqProcs.size(); i++) {
            TreatmentProcedureRequest pr = reqProcs.get(i);
            TreatmentProcedure target = pr.getId() != null ? existingById.get(pr.getId()) : null;
            if (target != null) {
                applyProcedureFields(target, pr, i);
                keepIds.add(target.getId());
            } else {
                plan.getProcedures().add(buildProcedure(plan, pr, i));
            }
        }

        plan.getProcedures().removeIf(p -> p.getId() != null && !keepIds.contains(p.getId()));
    }

    private void applyProcedureFields(TreatmentProcedure target, TreatmentProcedureRequest pr, int position) {
        target.setPosition(position);
        target.setTooth(pr.getTooth() == null || pr.getTooth().isBlank() ? "-" : pr.getTooth());
        target.setName(pr.getName());
        target.setValue(pr.getValue() == null ? BigDecimal.ZERO : pr.getValue());
        target.setPaid(Boolean.TRUE.equals(pr.getPaid()));
        target.setDone(Boolean.TRUE.equals(pr.getDone()));
    }

    @Transactional
    public TreatmentPlanResponse approveStep(UUID id) {
        TreatmentPlan plan = getOrThrow(id);

        TreatmentProcedure next = plan.getProcedures().stream()
                .filter(p -> !p.isDone())
                .findFirst()
                .orElseThrow(() -> new BusinessException("Não há etapas pendentes neste plano."));

        next.setDone(true);
        recalculateTotals(plan);
        return TreatmentPlanResponse.from(repository.save(plan));
    }

    @Transactional
    public void delete(UUID id) {
        TreatmentPlan plan = getOrThrow(id);
        plan.setDeletedAt(LocalDateTime.now());
        repository.save(plan);
    }

    private TreatmentPlan getOrThrow(UUID id) {
        return repository.findActiveById(id)
                .orElseThrow(() -> new BusinessException("Plano de tratamento não encontrado."));
    }

    private void validate(TreatmentPlanRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("O título do plano é obrigatório.");
        }
        if (request.getPatientId() == null
                && (request.getPatient() == null || request.getPatient().isBlank())) {
            throw new BusinessException("Informe o paciente (patientId ou patient).");
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("A data final não pode ser anterior à data inicial.");
        }
        List<TreatmentProcedureRequest> procs = request.getProcedures();
        if (procs == null || procs.isEmpty()) {
            throw new BusinessException("O plano deve ter ao menos um procedimento.");
        }
        for (TreatmentProcedureRequest pr : procs) {
            if (pr.getName() == null || pr.getName().isBlank()) {
                throw new BusinessException("Todo procedimento precisa de um nome.");
            }
            if (pr.getValue() == null || pr.getValue().signum() < 0) {
                throw new BusinessException("O valor do procedimento não pode ser negativo.");
            }
        }
    }

    private void bindPatient(TreatmentPlan plan, TreatmentPlanRequest request) {
        if (request.getPatientId() != null) {
            Patient patient = patientService.findById(request.getPatientId());
            plan.setPatient(patient);
            plan.setPatientName(patient.getName());
        } else {
            plan.setPatient(null);
            plan.setPatientName(request.getPatient());
        }
    }

    private TreatmentProcedure buildProcedure(TreatmentPlan plan, TreatmentProcedureRequest pr, int position) {
        TreatmentProcedure procedure = new TreatmentProcedure();
        procedure.setTreatmentPlan(plan);
        applyProcedureFields(procedure, pr, position);
        return procedure;
    }

    private void recalculateTotals(TreatmentPlan plan) {
        BigDecimal total = BigDecimal.ZERO;
        int done = 0;
        for (TreatmentProcedure p : plan.getProcedures()) {
            total = total.add(p.getValue() == null ? BigDecimal.ZERO : p.getValue());
            if (p.isDone()) done++;
        }
        plan.setTotal(total);
        plan.setCompleted(done);
        plan.setTotalProcedures(plan.getProcedures().size());
    }
}
