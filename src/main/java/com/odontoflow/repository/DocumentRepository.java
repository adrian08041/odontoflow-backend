package com.odontoflow.repository;

import com.odontoflow.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByPatientIdOrderByUploadedAtDesc(UUID patientId);
}
