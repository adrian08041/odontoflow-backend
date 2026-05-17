package com.odontoflow.repository;

import com.odontoflow.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
    Optional<Clinic> findFirstByOrderByCreatedAtAsc();
}
