package org.brian.aisupportagent.exception;

public class DocumentProcessingException extends RuntimeException {

    public DocumentProcessingException(Throwable cause) {
        super("The PDF could not be processed", cause);
    }
}
