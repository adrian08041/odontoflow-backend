package com.odontoflow.dto.response;

import com.odontoflow.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private UUID id;
    private String fileName;
    private String fileUrl;
    private String contentType;
    private long fileSizeBytes;
    private LocalDateTime uploadedAt;

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getFileName(),
                d.getFileUrl(),
                d.getContentType(),
                d.getFileSizeBytes(),
                d.getUploadedAt()
        );
    }
}
