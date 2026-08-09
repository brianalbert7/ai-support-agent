package org.brian.aisupportagent.exception;

import org.brian.aisupportagent.entity.DocumentStatus;

public class InvalidDocumentStateException extends RuntimeException {

    public InvalidDocumentStateException(DocumentStatus status) {
        super("A document with status " + status + " cannot be processed");
    }
}
