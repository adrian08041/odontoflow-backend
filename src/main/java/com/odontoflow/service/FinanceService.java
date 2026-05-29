package com.odontoflow.service;

import com.odontoflow.dto.request.FinanceStatusUpdateRequest;
import com.odontoflow.dto.request.NewTransactionRequest;
import com.odontoflow.dto.response.FinanceReceivableResponse;
import com.odontoflow.dto.response.FinanceStatsResponse;
import com.odontoflow.dto.response.PaymentMethodResponse;
import com.odontoflow.dto.response.RevenueHistoryResponse;
import com.odontoflow.entity.FinanceReceivable;
import com.odontoflow.entity.Patient;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.entity.enums.FinanceStatus;
import com.odontoflow.entity.enums.TransactionType;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.FinanceReceivableRepository;
import com.odontoflow.util.AuditChanges;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final String DEFAULT_EXPENSE_PAYER = "Clínica";

    private static final Map<String, String> METHOD_COLORS = Map.of(
            "PIX", "#10b981",
            "Cartão", "#3b82f6",
            "Cartao", "#3b82f6",
            "Boleto", "#f59e0b",
            "Dinheiro", "#6b7280",
            "Outros", "#a3a3a3"
    );

    private final FinanceReceivableRepository repository;
    private final PatientService patientService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<FinanceReceivableResponse> findAll(FinanceStatus status, TransactionType type, UUID patientId, Pageable pageable) {
        return repository.findAllFiltered(status, type, patientId, pageable).map(FinanceReceivableResponse::from);
    }

    public FinanceReceivable findById(UUID id) {
        return repository.findActiveById(id)
                .orElseThrow(() -> new BusinessException("Transação não encontrada."));
    }

    @Transactional
    public FinanceReceivableResponse create(NewTransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new BusinessException("O valor da transação deve ser maior que zero.");
        }
        if (request.getDueDate() == null) {
            throw new BusinessException("A data de vencimento é obrigatória.");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new BusinessException("A descrição da transação é obrigatória.");
        }

        FinanceReceivable receivable = new FinanceReceivable();
        bindPayer(receivable, request);

        receivable.setDescription(request.getDescription());
        receivable.setValue(request.getAmount());
        receivable.setDue(request.getDueDate());
        receivable.setType(request.getType());
        receivable.setMethod(request.getMethod());
        receivable.setCategory(request.getCategory());
        receivable.setInstallments(request.getInstallments());
        receivable.setNotes(request.getNotes());

        FinanceStatus initial = request.getStatus() != null
                ? request.getStatus()
                : resolveInitialStatus(request.getDueDate());
        receivable.setStatus(initial);
        if (initial == FinanceStatus.Pago) {
            receivable.setPaidAt(LocalDate.now());
        }

        FinanceReceivable saved = repository.save(receivable);
        auditLogService.log("FinanceReceivable", saved.getId(), AuditAction.CREATE,
                AuditChanges.after(AuditChanges.snapshot(saved)));
        return FinanceReceivableResponse.from(saved);
    }

    private void bindPayer(FinanceReceivable receivable, NewTransactionRequest request) {
        if (request.getPatientId() != null) {
            Patient patient = patientService.findById(request.getPatientId());
            receivable.setPatient(patient);
            receivable.setPatientName(patient.getName());
            return;
        }
        if (request.getPatient() != null && !request.getPatient().isBlank()) {
            receivable.setPatientName(request.getPatient().trim());
            return;
        }
        if (request.getType() == TransactionType.despesa) {
            receivable.setPatientName(DEFAULT_EXPENSE_PAYER);
            return;
        }
        throw new BusinessException("Informe o paciente (patientId ou patient).");
    }

    @Transactional
    public FinanceReceivableResponse updateStatus(UUID id, FinanceStatusUpdateRequest request) {
        if (request.getStatus() == null) {
            throw new BusinessException("O status é obrigatório.");
        }
        FinanceReceivable receivable = findById(id);
        Map<String, Object> before = Map.of(
                "status", String.valueOf(receivable.getStatus()),
                "paidAt", receivable.getPaidAt() != null ? receivable.getPaidAt().toString() : "null"
        );
        FinanceStatus target = request.getStatus();
        receivable.setStatus(target);
        if (target == FinanceStatus.Pago) {
            if (receivable.getPaidAt() == null) {
                receivable.setPaidAt(LocalDate.now());
            }
        } else {
            receivable.setPaidAt(null);
        }
        FinanceReceivable saved = repository.save(receivable);
        Map<String, Object> after = Map.of(
                "status", String.valueOf(saved.getStatus()),
                "paidAt", saved.getPaidAt() != null ? saved.getPaidAt().toString() : "null"
        );
        auditLogService.log("FinanceReceivable", saved.getId(), AuditAction.UPDATE,
                AuditChanges.diff(before, after));
        return FinanceReceivableResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        FinanceReceivable receivable = findById(id);
        Map<String, Object> before = AuditChanges.snapshot(receivable);
        receivable.setDeletedAt(LocalDateTime.now());
        repository.save(receivable);
        auditLogService.log("FinanceReceivable", receivable.getId(), AuditAction.DELETE,
                AuditChanges.before(before));
    }

    @Transactional(readOnly = true)
    public FinanceStatsResponse getStats() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        BigDecimal revenue = repository.sumRevenueBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        BigDecimal toReceive = repository.sumToReceiveBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        BigDecimal overdue = repository.sumOverdue();
        long paidCount = repository.countPaidBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        BigDecimal avgTicket = paidCount > 0
                ? revenue.divide(BigDecimal.valueOf(paidCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal previousRevenue = repository.sumRevenueBetween(previousMonth.atDay(1), previousMonth.atEndOfMonth());
        BigDecimal growth = calculateGrowth(revenue, previousRevenue);

        return new FinanceStatsResponse(revenue, toReceive, overdue, avgTicket, growth);
    }

    public RevenueHistoryResponse getRevenueHistory(int months) {
        if (months < 1) months = 6;
        if (months > 24) months = 24;

        List<String> labels = new ArrayList<>(months);
        List<BigDecimal> values = new ArrayList<>(months);

        YearMonth reference = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = reference.minusMonths(i);
            BigDecimal sum = repository.sumRevenueBetween(ym.atDay(1), ym.atEndOfMonth());
            labels.add(ym.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", ""));
            values.add(sum);
        }

        return new RevenueHistoryResponse(labels, values);
    }

    public List<PaymentMethodResponse> getPaymentMethodsDistribution() {
        List<Object[]> rows = repository.countByPaymentMethod();
        List<PaymentMethodResponse> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            String label = row[0] != null ? row[0].toString() : "Outros";
            Long count = ((Number) row[1]).longValue();
            String color = METHOD_COLORS.getOrDefault(label, "#a3a3a3");
            result.add(new PaymentMethodResponse(label, count, color));
        }
        return result;
    }

    @Scheduled(cron = "0 5 1 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void markOverdue() {
        LocalDate today = LocalDate.now();
        List<FinanceReceivable> overdue = repository.findOverdue(today);
        for (FinanceReceivable r : overdue) {
            r.setStatus(FinanceStatus.Atrasado);
        }
        if (!overdue.isEmpty()) {
            repository.saveAll(overdue);
            log.info("markOverdue: {} pendência(s) marcada(s) como Atrasado.", overdue.size());
        }
    }

    private FinanceStatus resolveInitialStatus(LocalDate dueDate) {
        return dueDate.isBefore(LocalDate.now()) ? FinanceStatus.Atrasado : FinanceStatus.Pendente;
    }

    private BigDecimal calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return current != null && current.signum() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }
}
