package com.odontoflow.controller;

import com.odontoflow.entity.Patient;
import com.odontoflow.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "CRUD de pacientes da clínica")
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @Operation(summary = "Listar pacientes", description = "Retorna todos os pacientes ativos (soft delete excluídos)")
    @ApiResponse(responseCode = "200", description = "Lista de pacientes retornada")
    public ResponseEntity<List<Patient>> findAll() {
        return ResponseEntity.ok(patientService.findAllActive());
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
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
