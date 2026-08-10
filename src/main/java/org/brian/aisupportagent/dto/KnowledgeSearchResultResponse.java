package org.brian.aisupportagent.dto;

import java.util.UUID;

public record KnowledgeSearchResultResponse(
        UUID chunkId,
        UUID documentId,
        String documentName,
        int pageNumber,
        int chunkIndex,
        String content,
        double similarity
) {
}
