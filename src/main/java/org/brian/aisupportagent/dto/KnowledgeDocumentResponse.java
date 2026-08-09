package org.brian.aisupportagent.dto;

import lombok.Builder;
import org.brian.aisupportagent.entity.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

@Builder
public record KnowledgeDocumentResponse(
        UUID id,
        String displayName,
        String originalFileName,
        String contentType,
        long sizeBytes,
        DocumentStatus status,
        Integer pageCount,
        UUID uploadedByUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
