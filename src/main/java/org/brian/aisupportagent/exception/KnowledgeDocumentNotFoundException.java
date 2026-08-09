package org.brian.aisupportagent.exception;

import java.util.UUID;

public class KnowledgeDocumentNotFoundException extends RuntimeException {

    public KnowledgeDocumentNotFoundException(UUID documentId) {
        super("Knowledge document not found: " + documentId);
    }
}
