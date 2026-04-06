package com.odontoflow.service;

import com.odontoflow.entity.Patient;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public Patient save(Patient patient){
        if (patientRepository.findByCpf(patient.getCpf()).isPresent()) {
            throw new BusinessException("Já existe um paciente cadastrado com este CPF.");
        }
        return patientRepository.save(patient);
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
        patient.setDeletedAt(java.time.LocalDateTime.now());
        patientRepository.save(patient);
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

        existing.setName(updatedData.getName());
        existing.setCpf(updatedData.getCpf());
        existing.setPhone(updatedData.getPhone());
        existing.setEmail(updatedData.getEmail());
        existing.setInsurance(updatedData.getInsurance());
        existing.setStatus(updatedData.getStatus());
        existing.setLastVisit(updatedData.getLastVisit());
        existing.setAvatar(updatedData.getAvatar());
        existing.setBirthDate(updatedData.getBirthDate());
        existing.setGender(updatedData.getGender());
        existing.setAddress(updatedData.getAddress());

        return patientRepository.save(existing);
    }
}
