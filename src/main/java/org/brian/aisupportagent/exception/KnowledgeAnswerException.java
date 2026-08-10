package org.brian.aisupportagent.exception;

public class KnowledgeAnswerException extends RuntimeException {

    public KnowledgeAnswerException(Throwable cause) {
        super("The knowledge answer could not be generated", cause);
    }
}
