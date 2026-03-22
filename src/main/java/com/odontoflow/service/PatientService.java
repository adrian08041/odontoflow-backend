package com.odontoflow.service;

import com.odontoflow.entity.Patient;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public List<Patient> findAllActive(){
        return patientRepository.findAll().stream()
                .filter(patient -> patient.getDeletedAt() == null)
                .toList();
    }

    public void delete (UUID id){
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Paciente não encontrado para exclusão."));
        patient.setDeletedAt(java.time.LocalDateTime.now());
        patientRepository.save(patient);
    }
}
