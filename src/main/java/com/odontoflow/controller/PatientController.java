package com.odontoflow.controller;

import com.odontoflow.entity.Patient;
import com.odontoflow.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "CRUD de pacientes da clínica")
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @Operation(summary = "Listar pacientes", description = "Retorna pacientes ativos com paginação, busca e filtros")
    @ApiResponse(responseCode = "200", description = "Lista paginada de pacientes")
    public ResponseEntity<Page<Patient>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String insurance,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(patientService.findAllActive(search, insurance, status, pageable));
    }

    @GetMapping("/insurances")
    @Operation(summary = "Listar seguros", description = "Retorna lista distinta de seguros cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de seguros")
    public ResponseEntity<java.util.List<String>> findInsurances() {
        return ResponseEntity.ok(patientService.findDistinctInsurances());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar paciente por ID", description = "Retorna um paciente específico pelo seu UUID")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "400", description = "Paciente não encontrado")
    public ResponseEntity<Patient> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(patientService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Criar paciente", description = "Cadastra um novo paciente. CPF deve ser único")
    @ApiResponse(responseCode = "201", description = "Paciente criado com sucesso")
    @ApiResponse(responseCode = "400", description = "CPF já cadastrado")
    public ResponseEntity<Patient> create(@RequestBody Patient patient) {
        Patient savedPatient = patientService.save(patient);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPatient);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover paciente", description = "Soft delete — marca deletedAt, não remove do banco")
    @ApiResponse(responseCode = "204", description = "Paciente removido com sucesso")
    @ApiResponse(responseCode = "400", description = "Paciente não encontrado")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {patientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar paciente", description = "Atualiza os dados de um paciente existente")
    @ApiResponse(responseCode = "200", description = "Paciente atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Paciente não encontrado ou CPF duplicado")
    public ResponseEntity<Patient> update(@PathVariable UUID id, @RequestBody Patient patient) {
        return ResponseEntity.ok(patientService.update(id, patient));
    }
}
