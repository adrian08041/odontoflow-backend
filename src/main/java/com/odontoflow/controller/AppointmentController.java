package com.odontoflow.controller;

import com.odontoflow.dto.request.AppointmentRequest;
import com.odontoflow.dto.request.RescheduleRequest;
import com.odontoflow.dto.request.StatusUpdateRequest;
import com.odontoflow.entity.Appointment;
import com.odontoflow.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "CRUD de agendamentos da clínica")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Listar agendamentos", description = "Lista agendamentos com filtros opcionais por intervalo de datas e dentista")
    @ApiResponse(responseCode = "200", description = "Lista de agendamentos retornada")
    public ResponseEntity<List<Appointment>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID dentistId
    ) {
        return ResponseEntity.ok(appointmentService.findAll(startDate, endDate, dentistId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar agendamento por ID", description = "Retorna um agendamento específico pelo seu UUID")
    @ApiResponse(responseCode = "200", description = "Agendamento encontrado")
    @ApiResponse(responseCode = "400", description = "Agendamento não encontrado")
    public ResponseEntity<Appointment> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Criar agendamento", description = "Cadastra um novo agendamento. Valida existência de paciente e dentista")
    @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Paciente/dentista não encontrado ou duração inválida")
    public ResponseEntity<Appointment> create(@RequestBody AppointmentRequest request) {
        Appointment created = appointmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar agendamento", description = "Atualiza todos os dados de um agendamento existente")
    @ApiResponse(responseCode = "200", description = "Agendamento atualizado")
    @ApiResponse(responseCode = "400", description = "Agendamento/paciente/dentista não encontrado")
    public ResponseEntity<Appointment> update(@PathVariable UUID id, @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.update(id, request));
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(summary = "Reagendar", description = "Atualiza apenas data e hora — usado no drag & drop da agenda")
    @ApiResponse(responseCode = "200", description = "Agendamento reagendado")
    @ApiResponse(responseCode = "400", description = "Agendamento não encontrado")
    public ResponseEntity<Appointment> reschedule(@PathVariable UUID id, @RequestBody RescheduleRequest request) {
        return ResponseEntity.ok(appointmentService.reschedule(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status", description = "Atualiza apenas o status (Confirmado/Pendente/Cancelado)")
    @ApiResponse(responseCode = "200", description = "Status atualizado")
    @ApiResponse(responseCode = "400", description = "Agendamento não encontrado")
    public ResponseEntity<Appointment> updateStatus(@PathVariable UUID id, @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar agendamento", description = "Soft delete — marca deletedAt, preserva histórico clínico")
    @ApiResponse(responseCode = "204", description = "Agendamento cancelado com sucesso")
    @ApiResponse(responseCode = "400", description = "Agendamento não encontrado")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
