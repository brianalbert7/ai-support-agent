package org.brian.aisupportagent.dto;

import java.util.List;

public record KnowledgeSearchResponse(
        String question,
        List<KnowledgeSearchResultResponse> results
) {

    public KnowledgeSearchResponse {
        results = List.copyOf(results);
    }
}
