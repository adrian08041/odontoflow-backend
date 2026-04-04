package com.odontoflow.repository;

import com.odontoflow.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("SELECT a FROM Appointment a WHERE a.deletedAt IS NULL AND " +
           "(:startDate IS NULL OR a.date >= :startDate) AND " +
           "(:endDate IS NULL OR a.date <= :endDate) AND " +
           "(:dentistId IS NULL OR a.dentist.id = :dentistId) " +
           "ORDER BY a.date ASC, a.time ASC")
    List<Appointment> findAllFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("dentistId") UUID dentistId
    );

    @Query("SELECT a FROM Appointment a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Appointment> findActiveById(@Param("id") UUID id);
}
