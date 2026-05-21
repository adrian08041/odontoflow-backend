package com.odontoflow.service;



import com.odontoflow.entity.Dentist;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.DentistRepository;
import com.odontoflow.util.AuditChanges;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DentistService {

    private final DentistRepository dentistRepository;
    private final AuditLogService auditLogService;

    public List<Dentist> findAll() {
        return dentistRepository.findAllActive();
    }

    public Dentist findById(UUID id) {
        return dentistRepository.findActiveById(id)
                .orElseThrow(() -> new BusinessException("Dentista não encontrado."));
    }

    public Dentist save(Dentist dentist) {
        Dentist saved = dentistRepository.save(dentist);
        auditLogService.log("Dentist", saved.getId(), AuditAction.CREATE,
                AuditChanges.after(AuditChanges.snapshot(saved)));
        return saved;
    }

    public Dentist update(UUID id, Dentist updatedData) {
        Dentist existing = findById(id);
        Map<String, Object> before = AuditChanges.snapshot(existing);
        existing.setName(updatedData.getName());
        existing.setSpecialty(updatedData.getSpecialty());
        Dentist saved = dentistRepository.save(existing);
        auditLogService.log("Dentist", saved.getId(), AuditAction.UPDATE,
                AuditChanges.diff(before, AuditChanges.snapshot(saved)));
        return saved;
    }

    public void delete(UUID id) {
        Dentist dentist = findById(id);
        Map<String, Object> before = AuditChanges.snapshot(dentist);
        dentist.setDeletedAt(LocalDateTime.now());
        dentistRepository.save(dentist);
        auditLogService.log("Dentist", id, AuditAction.DELETE,
                AuditChanges.before(before));
    }
}
