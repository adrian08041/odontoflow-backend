package com.odontoflow.service;

import com.odontoflow.entity.Patient;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.PatientRepository;
import com.odontoflow.util.AuditChanges;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final AuditLogService auditLogService;

    public Patient save(Patient patient){
        if (patientRepository.findByCpf(patient.getCpf()).isPresent()) {
            throw new BusinessException("Já existe um paciente cadastrado com este CPF.");
        }
        patient.setPhone(normalizePhone(patient.getPhone()));
        Patient saved = patientRepository.save(patient);
        auditLogService.log("Patient", saved.getId(), AuditAction.CREATE,
                AuditChanges.after(AuditChanges.snapshot(saved)));
        return saved;
    }

    public Page<Patient> findAllActive(String search, String insurance, String status, Pageable pageable) {
        String searchParam = (search == null || search.isBlank()) ? null : search;
        String insuranceParam = (insurance == null || insurance.isBlank()) ? null : insurance;
        String statusParam = (status == null || status.isBlank()) ? null : status;
        return patientRepository.findFiltered(searchParam, insuranceParam, statusParam, pageable);
    }

    public java.util.List<String> findDistinctInsurances() {
        return patientRepository.findDistinctInsurances();
    }

    public void delete (UUID id){
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Paciente não encontrado para exclusão."));
        Map<String, Object> before = AuditChanges.snapshot(patient);
        patient.setDeletedAt(java.time.LocalDateTime.now());
        patientRepository.save(patient);
        auditLogService.log("Patient", patient.getId(), AuditAction.DELETE,
                AuditChanges.before(before));
    }
    public Patient findById(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Paciente não encontrado."));

        if (patient.getDeletedAt() != null) {
            throw new BusinessException("Paciente não encontrado.");
        }

        return patient;
    }

    public Patient update(UUID id, Patient updatedData) {
        Patient existing = findById(id);

        if (!existing.getCpf().equals(updatedData.getCpf())) {
            patientRepository.findByCpf(updatedData.getCpf())
                    .ifPresent(p -> {
                        throw new BusinessException("Já existe um paciente cadastrado com este CPF.");
                    });
        }

        Map<String, Object> before = AuditChanges.snapshot(existing);

        existing.setName(updatedData.getName());
        existing.setCpf(updatedData.getCpf());
        existing.setPhone(normalizePhone(updatedData.getPhone()));
        existing.setEmail(updatedData.getEmail());
        existing.setInsurance(updatedData.getInsurance());
        existing.setStatus(updatedData.getStatus());
        existing.setLastVisit(updatedData.getLastVisit());
        existing.setAvatar(updatedData.getAvatar());
        existing.setBirthDate(updatedData.getBirthDate());
        existing.setGender(updatedData.getGender());
        existing.setAddress(updatedData.getAddress());

        Patient saved = patientRepository.save(existing);
        auditLogService.log("Patient", saved.getId(), AuditAction.UPDATE,
                AuditChanges.diff(before, AuditChanges.snapshot(saved)));
        return saved;
    }

    /**
     * Normaliza telefone para o formato esperado pelo uazapi (E.164 sem '+').
     * Heurística conservadora — só prefixa DDI 55 quando o número parece BR.
     * Números estrangeiros já formatados são preservados sem corrupção.
     */
    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;

        // Já tem DDI 55 + 10 ou 11 dígitos locais (12 ou 13 total) → BR completo
        if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
            return digits;
        }
        // 10 ou 11 dígitos sem DDI → assume BR e prefixa
        if (digits.length() == 10 || digits.length() == 11) {
            return "55" + digits;
        }
        // Outros formatos (internacional, malformado) → mantém como veio
        return digits;
    }
}
