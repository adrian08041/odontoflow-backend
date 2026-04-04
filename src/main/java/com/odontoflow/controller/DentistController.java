package com.odontoflow.controller;

import com.odontoflow.entity.Dentist;
import com.odontoflow.service.DentistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dentists")
@RequiredArgsConstructor
@Tag(name = "Dentistas", description = "CRUD de dentistas da clínica")
public class DentistController {

    private final DentistService dentistService;

    @GetMapping
    @Operation(summary = "Listar dentistas", description = "Retorna todos os dentistas cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista de dentistas retornada")
    public ResponseEntity<List<Dentist>> findAll() {
        return ResponseEntity.ok(dentistService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar dentista por ID", description = "Retorna um dentista específico pelo seu UUID")
    @ApiResponse(responseCode = "200", description = "Dentista encontrado")
    @ApiResponse(responseCode = "400", description = "Dentista não encontrado")
    public ResponseEntity<Dentist> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(dentistService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Criar dentista", description = "Cadastra um novo dentista")
    @ApiResponse(responseCode = "201", description = "Dentista criado com sucesso")
    public ResponseEntity<Dentist> create(@RequestBody Dentist dentist) {
        Dentist saved = dentistService.save(dentist);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dentista", description = "Atualiza os dados de um dentista existente")
    @ApiResponse(responseCode = "200", description = "Dentista atualizado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dentista não encontrado")
    public ResponseEntity<Dentist> update(@PathVariable UUID id, @RequestBody Dentist dentist) {
        return ResponseEntity.ok(dentistService.update(id, dentist));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover dentista", description = "Soft delete — marca deletedAt, não remove do banco")
    @ApiResponse(responseCode = "204", description = "Dentista removido com sucesso")
    @ApiResponse(responseCode = "400", description = "Dentista não encontrado")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dentistService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
