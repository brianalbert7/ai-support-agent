package org.brian.aisupportagent.dto;

import java.util.List;

public record KnowledgeAnswerResponse(
        String question,
        String answer,
        boolean grounded,
        List<KnowledgeCitationResponse> citations
) {

    public KnowledgeAnswerResponse {
        citations = List.copyOf(citations);
    }
}
