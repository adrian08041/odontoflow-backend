package com.odontoflow.repository;

import com.odontoflow.entity.FinanceReceivable;
import com.odontoflow.entity.enums.FinanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceReceivableRepository extends JpaRepository<FinanceReceivable, UUID> {

    @Query("SELECT r FROM FinanceReceivable r WHERE r.deletedAt IS NULL AND " +
           "(:status IS NULL OR r.status = :status) " +
           "ORDER BY r.due DESC")
    Page<FinanceReceivable> findAllFiltered(
            @Param("status") FinanceStatus status,
            Pageable pageable
    );

    @Query("SELECT r FROM FinanceReceivable r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<FinanceReceivable> findActiveById(@Param("id") UUID id);

    @Query("SELECT r FROM FinanceReceivable r WHERE r.deletedAt IS NULL AND " +
           "r.status = com.odontoflow.entity.enums.FinanceStatus.Pendente AND " +
           "r.due < :today")
    List<FinanceReceivable> findOverdue(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(r.value), 0) FROM FinanceReceivable r WHERE r.deletedAt IS NULL AND " +
           "r.status = com.odontoflow.entity.enums.FinanceStatus.Pago AND " +
           "r.due BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(r.value), 0) FROM FinanceReceivable r WHERE r.deletedAt IS NULL AND " +
           "r.status = com.odontoflow.entity.enums.FinanceStatus.Pendente AND " +
           "r.due BETWEEN :start AND :end")
    BigDecimal sumToReceiveBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(r.value), 0) FROM FinanceReceivable r WHERE r.deletedAt IS NULL AND " +
           "r.status = com.odontoflow.entity.enums.FinanceStatus.Atrasado")
    BigDecimal sumOverdue();

    @Query("SELECT COUNT(r) FROM FinanceReceivable r WHERE r.deletedAt IS NULL AND " +
           "r.status = com.odontoflow.entity.enums.FinanceStatus.Atrasado")
    long countOverdue();

    @Query("SELECT COUNT(r) FROM FinanceReceivable r WHERE r.deletedAt IS NULL AND " +
           "r.status = com.odontoflow.entity.enums.FinanceStatus.Pago AND " +
           "r.due BETWEEN :start AND :end")
    long countPaidBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(r.method, 'Outros') as method, COUNT(r) as qtd " +
           "FROM FinanceReceivable r WHERE r.deletedAt IS NULL AND " +
           "r.status = com.odontoflow.entity.enums.FinanceStatus.Pago " +
           "GROUP BY r.method")
    List<Object[]> countByPaymentMethod();
}
