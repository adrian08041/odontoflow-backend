package com.odontoflow.controller;

import com.odontoflow.dto.request.FinanceStatusUpdateRequest;
import com.odontoflow.dto.request.NewTransactionRequest;
import com.odontoflow.dto.response.FinanceReceivableResponse;
import com.odontoflow.dto.response.FinanceStatsResponse;
import com.odontoflow.dto.response.PaymentMethodResponse;
import com.odontoflow.dto.response.RevenueHistoryResponse;
import com.odontoflow.entity.enums.FinanceStatus;
import com.odontoflow.entity.enums.TransactionType;
import com.odontoflow.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
@Tag(name = "Financeiro", description = "Contas a receber, transações e métricas financeiras")
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/receivables")
    @Operation(summary = "Listar contas a receber", description = "Retorna paginado com filtros opcionais por status (Pendente, Pago, Atrasado) e tipo (receita, despesa)")
    @ApiResponse(responseCode = "200", description = "Lista paginada de contas a receber")
    public ResponseEntity<Page<FinanceReceivableResponse>> findAll(
            @RequestParam(required = false) FinanceStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(financeService.findAll(status, type, pageable));
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Nova transação", description = "Cria uma nova conta a receber/despesa. Status é calculado pela data de vencimento")
    @ApiResponse(responseCode = "201", description = "Transação criada com sucesso")
    @ApiResponse(responseCode = "400", description = "Valor inválido, paciente não encontrado, descrição/data ausente")
    public ResponseEntity<FinanceReceivableResponse> create(@RequestBody NewTransactionRequest request) {
        FinanceReceivableResponse created = financeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/transactions/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar status", description = "Muda o status da transação para Pendente, Pago ou Atrasado")
    @ApiResponse(responseCode = "200", description = "Status atualizado")
    @ApiResponse(responseCode = "400", description = "Transação não encontrada ou status inválido")
    public ResponseEntity<FinanceReceivableResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody FinanceStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(financeService.updateStatus(id, request));
    }

    @DeleteMapping("/transactions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Excluir transação", description = "Soft delete — preserva o histórico contábil")
    @ApiResponse(responseCode = "204", description = "Transação excluída")
    @ApiResponse(responseCode = "400", description = "Transação não encontrada")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        financeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Métricas do mês", description = "Receita, a receber, atrasados, ticket médio e crescimento em relação ao mês anterior")
    @ApiResponse(responseCode = "200", description = "Métricas calculadas")
    public ResponseEntity<FinanceStatsResponse> getStats() {
        return ResponseEntity.ok(financeService.getStats());
    }

    @GetMapping("/revenue-history")
    @Operation(summary = "Histórico de faturamento mensal", description = "Soma de receitas pagas por mês — usado em gráficos")
    @ApiResponse(responseCode = "200", description = "Histórico retornado")
    public ResponseEntity<RevenueHistoryResponse> getRevenueHistory(
            @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(financeService.getRevenueHistory(months));
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "Distribuição por método de pagamento", description = "Contagem de transações pagas agrupadas por método")
    @ApiResponse(responseCode = "200", description = "Distribuição calculada")
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods() {
        return ResponseEntity.ok(financeService.getPaymentMethodsDistribution());
    }
}
