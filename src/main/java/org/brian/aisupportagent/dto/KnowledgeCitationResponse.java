package org.brian.aisupportagent.dto;

import java.util.UUID;

public record KnowledgeCitationResponse(
        int sourceNumber,
        UUID chunkId,
        UUID documentId,
        String documentName,
        int pageNumber,
        String excerpt,
        double similarity
) {
}
