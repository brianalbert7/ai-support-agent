package org.brian.aisupportagent.exception;

public class DocumentDeletionException extends RuntimeException {

    public DocumentDeletionException(Throwable cause) {
        super("The document could not be deleted", cause);
    }
}
