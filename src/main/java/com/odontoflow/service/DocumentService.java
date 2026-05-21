package com.odontoflow.service;

import com.odontoflow.dto.response.DocumentResponse;
import com.odontoflow.entity.Document;
import com.odontoflow.entity.Patient;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.DocumentRepository;
import com.odontoflow.util.AuditChanges;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );

    private final DocumentRepository documentRepository;
    private final PatientService patientService;
    private final SupabaseStorageService storageService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<DocumentResponse> listByPatient(UUID patientId) {
        patientService.findById(patientId);
        return documentRepository.findByPatientIdOrderByUploadedAtDesc(patientId)
                .stream().map(DocumentResponse::from).toList();
    }

    @Transactional
    public DocumentResponse upload(UUID patientId, MultipartFile file) {
        validateFile(file);
        Patient patient = patientService.findById(patientId);

        String safeName = sanitize(file.getOriginalFilename());
        String path = "patients/" + patientId + "/" + UUID.randomUUID() + "-" + safeName;

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException("Falha ao ler o arquivo enviado.");
        }
        String publicUrl = storageService.upload(bytes, path, file.getContentType());

        Document doc = new Document();
        doc.setPatient(patient);
        doc.setFileName(safeName);
        doc.setStoragePath(path);
        doc.setFileUrl(publicUrl);
        doc.setContentType(file.getContentType());
        doc.setFileSizeBytes(file.getSize());
        Document saved = documentRepository.save(doc);
        auditLogService.log("Document", saved.getId(), AuditAction.CREATE,
                AuditChanges.after(AuditChanges.snapshot(saved)));
        return DocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Document findById(UUID patientId, UUID docId) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException("Documento não encontrado."));
        if (!doc.getPatient().getId().equals(patientId)) {
            throw new BusinessException("Documento não pertence a este paciente.");
        }
        return doc;
    }

    @Transactional
    public void delete(UUID patientId, UUID docId) {
        Document doc = findById(patientId, docId);
        var before = AuditChanges.snapshot(doc);
        storageService.delete(doc.getStoragePath());
        documentRepository.delete(doc);
        auditLogService.log("Document", docId, AuditAction.DELETE,
                AuditChanges.before(before));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Envie um arquivo.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("Arquivo excede o tamanho máximo de 5 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Tipo de arquivo não permitido. Aceitos: PDF, PNG, JPG, JPEG, WEBP.");
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) return "arquivo";
        String cleaned = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
    }
}
