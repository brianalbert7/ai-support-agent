package org.brian.aisupportagent.service;

import java.util.UUID;

public record RetrievedDocumentChunk(
        UUID chunkId,
        UUID documentId,
        String documentName,
        int pageNumber,
        int chunkIndex,
        String content,
        double similarity
) {
}
