package com.odontoflow.repository;

import com.odontoflow.entity.TreatmentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, UUID> {

    @Query("SELECT p FROM TreatmentPlan p WHERE p.deletedAt IS NULL AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.patientName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.createdAt DESC, p.id DESC")
    Page<TreatmentPlan> findAllFiltered(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM TreatmentPlan p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<TreatmentPlan> findActiveById(@Param("id") UUID id);

    @Query("SELECT COUNT(p) FROM TreatmentPlan p WHERE p.deletedAt IS NULL AND " +
           "p.endDate IS NOT NULL AND p.endDate < :today AND " +
           "p.completed < p.totalProcedures")
    long countOverdue(@Param("today") LocalDate today);

    @Query("SELECT COUNT(p) FROM TreatmentPlan p WHERE p.deletedAt IS NULL AND " +
           "p.totalProcedures > 0 AND p.completed >= p.totalProcedures AND " +
           "p.completedAt IS NOT NULL AND " +
           "p.completedAt >= :start AND p.completedAt < :end")
    long countCompletedBetween(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );
}
