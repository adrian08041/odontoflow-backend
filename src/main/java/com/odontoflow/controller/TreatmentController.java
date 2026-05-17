package com.odontoflow.controller;

import com.odontoflow.dto.request.TreatmentPlanRequest;
import com.odontoflow.dto.response.TreatmentPlanResponse;
import com.odontoflow.service.TreatmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/treatments")
@RequiredArgsConstructor
@Tag(name = "Tratamentos", description = "Planos de tratamento e procedimentos")
public class TreatmentController {

    private final TreatmentService treatmentService;

    @GetMapping
    @Operation(summary = "Listar planos", description = "Paginado, busca opcional por título ou nome do paciente")
    @ApiResponse(responseCode = "200", description = "Lista paginada de planos")
    public ResponseEntity<Page<TreatmentPlanResponse>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(treatmentService.findAll(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes do plano", description = "Retorna o plano com seus procedimentos")
    @ApiResponse(responseCode = "200", description = "Plano encontrado")
    @ApiResponse(responseCode = "400", description = "Plano não encontrado")
    public ResponseEntity<TreatmentPlanResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(treatmentService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Criar plano", description = "Cria um plano com seus procedimentos. Total/completed/totalProcedures são calculados.")
    @ApiResponse(responseCode = "201", description = "Plano criado")
    @ApiResponse(responseCode = "400", description = "Validação falhou (título, paciente, datas ou procedimentos)")
    public ResponseEntity<TreatmentPlanResponse> create(@RequestBody TreatmentPlanRequest request) {
        TreatmentPlanResponse created = treatmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar plano", description = "Substitui dados e procedimentos do plano")
    @ApiResponse(responseCode = "200", description = "Plano atualizado")
    @ApiResponse(responseCode = "400", description = "Plano não encontrado ou validação falhou")
    public ResponseEntity<TreatmentPlanResponse> update(
            @PathVariable UUID id,
            @RequestBody TreatmentPlanRequest request
    ) {
        return ResponseEntity.ok(treatmentService.update(id, request));
    }

    @PatchMapping("/{id}/approve-step")
    @Operation(summary = "Aprovar etapa", description = "Marca o próximo procedimento pendente como concluído")
    @ApiResponse(responseCode = "200", description = "Etapa aprovada e progresso atualizado")
    @ApiResponse(responseCode = "400", description = "Plano não encontrado ou nenhuma etapa pendente")
    public ResponseEntity<TreatmentPlanResponse> approveStep(@PathVariable UUID id) {
        return ResponseEntity.ok(treatmentService.approveStep(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir plano", description = "Soft delete — preserva histórico clínico")
    @ApiResponse(responseCode = "204", description = "Plano excluído")
    @ApiResponse(responseCode = "400", description = "Plano não encontrado")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        treatmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
