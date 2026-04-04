package com.odontoflow.service;



import com.odontoflow.entity.Dentist;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.DentistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DentistService {

    private final DentistRepository dentistRepository;

    public List<Dentist> findAll() {
        return dentistRepository.findAllActive();
    }

    public Dentist findById(UUID id) {
        return dentistRepository.findActiveById(id)
                .orElseThrow(() -> new BusinessException("Dentista não encontrado."));
    }

    public Dentist save(Dentist dentist) {
        return dentistRepository.save(dentist);
    }

    public Dentist update(UUID id, Dentist updatedData) {
        Dentist existing = findById(id);
        existing.setName(updatedData.getName());
        existing.setSpecialty(updatedData.getSpecialty());
        return dentistRepository.save(existing);
    }

    public void delete(UUID id) {
        Dentist dentist = findById(id);
        dentist.setDeletedAt(LocalDateTime.now());
        dentistRepository.save(dentist);
    }
}