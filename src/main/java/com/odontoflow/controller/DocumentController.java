package com.odontoflow.controller;

import com.odontoflow.dto.response.DocumentResponse;
import com.odontoflow.entity.Document;
import com.odontoflow.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients/{patientId}/documents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DENTISTA', 'RECEPCIONISTA')")
@Tag(name = "Documentos", description = "Documentos do paciente (PDF, PNG, JPG, JPEG, WEBP — até 5 MB)")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "Listar documentos", description = "Documentos do paciente ordenados por upload mais recente")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    @ApiResponse(responseCode = "400", description = "Paciente não encontrado")
    public ResponseEntity<List<DocumentResponse>> list(@PathVariable UUID patientId) {
        return ResponseEntity.ok(documentService.listByPatient(patientId));
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Upload de documento", description = "multipart/form-data com campo 'file'. Aceitos: PDF, PNG, JPG, JPEG, WEBP. Máx. 5 MB.")
    @ApiResponse(responseCode = "201", description = "Documento criado")
    @ApiResponse(responseCode = "400", description = "Arquivo inválido, tipo não permitido, tamanho excedido ou paciente não encontrado")
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable UUID patientId,
            @RequestParam("file") MultipartFile file
    ) {
        DocumentResponse created = documentService.upload(patientId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{docId}")
    @Operation(summary = "Download de documento", description = "Redireciona para a URL pública do arquivo no storage")
    @ApiResponse(responseCode = "302", description = "Redirect para a URL pública")
    @ApiResponse(responseCode = "400", description = "Documento não encontrado ou não pertence ao paciente")
    public ResponseEntity<Void> download(@PathVariable UUID patientId, @PathVariable UUID docId) {
        Document doc = documentService.findById(patientId, docId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, doc.getFileUrl())
                .build();
    }

    @DeleteMapping("/{docId}")
    @Operation(summary = "Remover documento", description = "Remove do storage e do banco")
    @ApiResponse(responseCode = "204", description = "Documento removido")
    @ApiResponse(responseCode = "400", description = "Documento não encontrado ou não pertence ao paciente")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID docId) {
        documentService.delete(patientId, docId);
        return ResponseEntity.noContent().build();
    }
}
