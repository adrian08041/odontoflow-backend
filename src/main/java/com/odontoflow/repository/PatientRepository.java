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

    /**
     * Pacientes cujo CPF — comparado só por dígitos — bate com :digits. Robusto a máscara
     * (000.000.000-00) vs dígitos puros. NÃO filtra deletedAt: um CPF de paciente soft-deleted
     * também conta como duplicado pro autocadastro do bot (→ handoff). Retorna List porque o banco
     * pode ter o mesmo CPF em formatos diferentes (seed vs front) — evita NonUniqueResultException.
     */
    @Query("SELECT p FROM Patient p WHERE REPLACE(REPLACE(p.cpf, '.', ''), '-', '') = :digits")
    java.util.List<Patient> findByCpfDigits(@Param("digits") String digits);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL")
    Page<Patient> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            +
            "p.cpf LIKE CONCAT('%', :search, '%'))")
    Page<Patient> searchActive(@Param("search") String search, Pageable pageable);

    // Status derivado da atividade (não da coluna armazenada, que não é mantida):
    // Ativo ⟺ existe consulta não-cancelada com date >= threshold (hoje − janela de inatividade),
    // o que cobre tanto visita recente quanto consulta futura. statusActive: null = sem filtro,
    // TRUE = só ativos, FALSE = só inativos.
    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
            "(:search IS NULL OR LOWER(CAST(p.name AS String)) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR CAST(p.cpf AS String) LIKE CONCAT('%', CAST(:search AS String), '%')) AND " +
            "(:insurance IS NULL OR p.insurance = CAST(:insurance AS String)) AND " +
            "(:statusActive IS NULL OR " +
            " (:statusActive = TRUE AND EXISTS (SELECT 1 FROM Appointment a WHERE a.patient = p AND a.deletedAt IS NULL AND a.status <> 'Cancelado' AND a.date >= :threshold)) OR " +
            " (:statusActive = FALSE AND NOT EXISTS (SELECT 1 FROM Appointment a WHERE a.patient = p AND a.deletedAt IS NULL AND a.status <> 'Cancelado' AND a.date >= :threshold)))")
    Page<Patient> findFiltered(@Param("search") String search,
                               @Param("insurance") String insurance,
                               @Param("statusActive") Boolean statusActive,
                               @Param("threshold") java.time.LocalDate threshold,
                               Pageable pageable);

    @Query("SELECT DISTINCT p.insurance FROM Patient p WHERE p.deletedAt IS NULL AND p.insurance IS NOT NULL ORDER BY p.insurance")
    java.util.List<String> findDistinctInsurances();

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND p.birthDate IS NOT NULL AND " +
           "EXTRACT(MONTH FROM p.birthDate) = :month AND " +
           "EXTRACT(DAY FROM p.birthDate) = :day")
    java.util.List<Patient> findBirthdaysOn(@Param("month") int month, @Param("day") int day);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.deletedAt IS NULL AND " +
           "p.createdAt >= :start AND p.createdAt < :end")
    long countCreatedBetween(@Param("start") java.time.LocalDateTime start,
                             @Param("end") java.time.LocalDateTime end);
}
