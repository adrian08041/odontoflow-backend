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

    // CAST(:x AS date) nos params temporais: o PostgreSQL não infere o tipo de um
    // parâmetro usado só em `IS NULL` e falha na preparação do statement com
    // "could not determine data type of parameter" — independe do valor passado.
    // UUID/String não precisam (mesmo padrão do AuditLogRepository.findFiltered).
    @Query("SELECT a FROM Appointment a WHERE a.deletedAt IS NULL AND " +
           "(CAST(:startDate AS date) IS NULL OR a.date >= :startDate) AND " +
           "(CAST(:endDate AS date) IS NULL OR a.date <= :endDate) AND " +
           "(:dentistId IS NULL OR a.dentist.id = :dentistId) " +
           "ORDER BY a.date ASC, a.time ASC")
    List<Appointment> findAllFiltered(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("dentistId") UUID dentistId
    );

    // Queries dedicadas do bot WhatsApp (Fase 2) — sem o padrão ':param IS NULL',
    // que no PostgreSQL falha com "could not determine data type of parameter".
    @Query("SELECT a FROM Appointment a WHERE a.deletedAt IS NULL " +
           "AND a.date = :date AND a.dentist.id = :dentistId " +
           "ORDER BY a.time ASC")
    List<Appointment> findActiveByDateAndDentist(
            @Param("date") LocalDate date,
            @Param("dentistId") UUID dentistId
    );

    @Query("SELECT a FROM Appointment a WHERE a.deletedAt IS NULL " +
           "AND a.patient.id = :patientId AND a.date >= :from " +
           "AND a.status <> 'Cancelado' " +
           "ORDER BY a.date ASC, a.time ASC")
    List<Appointment> findUpcomingActiveByPatient(
            @Param("patientId") UUID patientId,
            @Param("from") LocalDate from
    );

    @Query("SELECT a FROM Appointment a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Appointment> findActiveById(@Param("id") UUID id);

    @Query("SELECT a FROM Appointment a WHERE a.deletedAt IS NULL AND a.date = :date " +
           "ORDER BY a.time ASC")
    List<Appointment> findByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.deletedAt IS NULL AND a.date = :date")
    long countByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.deletedAt IS NULL AND a.date = :date AND a.status = :status")
    long countByDateAndStatus(@Param("date") LocalDate date, @Param("status") String status);

    @Query("SELECT a.date as dia, COUNT(a) as qtd FROM Appointment a WHERE a.deletedAt IS NULL AND " +
           "a.date BETWEEN :start AND :end GROUP BY a.date")
    List<Object[]> countByDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.deletedAt IS NULL AND " +
           "a.date = :date AND a.status = 'Pendente'")
    long countUnconfirmedOn(@Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.deletedAt IS NULL AND " +
           "a.type = com.odontoflow.entity.enums.AppointmentType.RETURN AND " +
           "a.date >= :today AND a.status = 'Pendente'")
    long countPendingReturnsFrom(@Param("today") LocalDate today);

    @Query("SELECT a FROM Appointment a WHERE a.deletedAt IS NULL " +
           "AND a.reminderSentAt IS NULL " +
           "AND a.status = 'Pendente' " +
           "AND a.patient.phone IS NOT NULL " +
           "AND a.date BETWEEN :startDate AND :endDate " +
           "ORDER BY a.date ASC, a.time ASC")
    List<Appointment> findPendingReminders(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
