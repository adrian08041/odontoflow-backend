package com.odontoflow.controller;

import com.odontoflow.dto.request.ClinicRequest;
import com.odontoflow.dto.request.HoursRequest;
import com.odontoflow.dto.request.InsuranceRequest;
import com.odontoflow.dto.response.ClinicResponse;
import com.odontoflow.dto.response.HoursResponse;
import com.odontoflow.dto.response.InsuranceResponse;
import com.odontoflow.dto.response.LogoUploadResponse;
import com.odontoflow.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Configurações", description = "Dados da clínica, horários de funcionamento e convênios")
public class SettingsController {

    private final SettingsService settingsService;

    // ---------- Clínica ----------

    @GetMapping("/clinic")
    @Operation(summary = "Dados da clínica", description = "Retorna o registro único da clínica. Cria default na primeira chamada.")
    @ApiResponse(responseCode = "200", description = "Dados retornados")
    public ResponseEntity<ClinicResponse> getClinic() {
        return ResponseEntity.ok(settingsService.getClinic());
    }

    @PutMapping("/clinic")
    @Operation(summary = "Atualizar dados da clínica", description = "Edita nomeFantasia, cnpj, telefone, endereço, email, website e logoUrl")
    @ApiResponse(responseCode = "200", description = "Clínica atualizada")
    @ApiResponse(responseCode = "400", description = "Validação falhou")
    public ResponseEntity<ClinicResponse> updateClinic(@RequestBody ClinicRequest request) {
        return ResponseEntity.ok(settingsService.updateClinic(request));
    }

    @PostMapping(value = "/clinic/logo", consumes = "multipart/form-data")
    @Operation(summary = "Upload do logo", description = "multipart/form-data com campo 'file'. Aceitos: PNG, JPG, JPEG, WEBP. Máx. 2 MB. Logo anterior é removido do storage.")
    @ApiResponse(responseCode = "200", description = "Logo enviado")
    @ApiResponse(responseCode = "400", description = "Arquivo inválido")
    public ResponseEntity<LogoUploadResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(settingsService.uploadLogo(file));
    }

    // ---------- Horários ----------

    @GetMapping("/hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Horários de funcionamento", description = "Dias da semana + duração da consulta + intervalo")
    @ApiResponse(responseCode = "200", description = "Horários retornados")
    public ResponseEntity<HoursResponse> getHours() {
        return ResponseEntity.ok(settingsService.getHours());
    }

    @PutMapping("/hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Atualizar horários", description = "Substitui completamente a lista de dias e atualiza duração/intervalo")
    @ApiResponse(responseCode = "200", description = "Horários atualizados")
    @ApiResponse(responseCode = "400", description = "Validação falhou (lista vazia, duração/intervalo ausentes)")
    public ResponseEntity<HoursResponse> updateHours(@RequestBody HoursRequest request) {
        return ResponseEntity.ok(settingsService.updateHours(request));
    }

    // ---------- Convênios ----------

    @GetMapping("/insurances")
    @PreAuthorize("hasAnyRole('ADMIN', 'DENTISTA', 'RECEPCIONISTA')")
    @Operation(summary = "Listar convênios", description = "Convênios cadastrados, ordenados por nome. Leitura liberada a todos os cargos (usado no cadastro de paciente)")
    @ApiResponse(responseCode = "200", description = "Lista de convênios")
    public ResponseEntity<List<InsuranceResponse>> listInsurances() {
        return ResponseEntity.ok(settingsService.listInsurances());
    }

    @PostMapping("/insurances")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Adicionar convênio", description = "Cria um novo convênio. Status default = Ativo")
    @ApiResponse(responseCode = "201", description = "Convênio criado")
    @ApiResponse(responseCode = "400", description = "Nome obrigatório")
    public ResponseEntity<InsuranceResponse> addInsurance(@RequestBody InsuranceRequest request) {
        InsuranceResponse created = settingsService.addInsurance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/insurances/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Atualizar convênio", description = "Atualização parcial — só altera campos não nulos")
    @ApiResponse(responseCode = "200", description = "Convênio atualizado")
    @ApiResponse(responseCode = "400", description = "Convênio não encontrado")
    public ResponseEntity<InsuranceResponse> updateInsurance(
            @PathVariable UUID id,
            @RequestBody InsuranceRequest request
    ) {
        return ResponseEntity.ok(settingsService.updateInsurance(id, request));
    }

    @DeleteMapping("/insurances/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Operation(summary = "Remover convênio", description = "Deleção física (catálogo, sem soft delete)")
    @ApiResponse(responseCode = "204", description = "Convênio removido")
    @ApiResponse(responseCode = "400", description = "Convênio não encontrado")
    public ResponseEntity<Void> deleteInsurance(@PathVariable UUID id) {
        settingsService.deleteInsurance(id);
        return ResponseEntity.noContent().build();
    }
}
