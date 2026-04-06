package com.odontoflow.repository;

import com.odontoflow.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByCpf(String cpf);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL")
    Page<Patient> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            +
            "p.cpf LIKE CONCAT('%', :search, '%'))")
    Page<Patient> searchActive(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
            "(:search IS NULL OR LOWER(CAST(p.name AS String)) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR CAST(p.cpf AS String) LIKE CONCAT('%', CAST(:search AS String), '%')) AND " +
            "(:insurance IS NULL OR p.insurance = CAST(:insurance AS String)) AND " +
            "(:status IS NULL OR p.status = CAST(:status AS String))")
    Page<Patient> findFiltered(@Param("search") String search,
                               @Param("insurance") String insurance,
                               @Param("status") String status,
                               Pageable pageable);

    @Query("SELECT DISTINCT p.insurance FROM Patient p WHERE p.deletedAt IS NULL AND p.insurance IS NOT NULL ORDER BY p.insurance")
    java.util.List<String> findDistinctInsurances();
}
