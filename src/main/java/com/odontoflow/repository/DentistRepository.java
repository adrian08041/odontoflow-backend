package com.odontoflow.repository;

import com.odontoflow.entity.Dentist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DentistRepository extends JpaRepository<Dentist, UUID> {

    @Query("SELECT d FROM Dentist d WHERE d.deletedAt IS NULL")
    List<Dentist> findAllActive();

    @Query("SELECT d FROM Dentist d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Dentist> findActiveById(@org.springframework.data.repository.query.Param("id") UUID id);
}
