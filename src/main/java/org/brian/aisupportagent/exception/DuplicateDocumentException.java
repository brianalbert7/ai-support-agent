package org.brian.aisupportagent.exception;

public class DuplicateDocumentException extends RuntimeException {

    public DuplicateDocumentException() {
        super("This document has already been uploaded");
    }
}
