package org.brian.aisupportagent.exception;

public class KnowledgeSearchException extends RuntimeException {

    public KnowledgeSearchException(Throwable cause) {
        super("Knowledge search is temporarily unavailable", cause);
    }
}
