package org.brian.aisupportagent.dto;

import org.brian.aisupportagent.entity.ConversationMessageRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID id,
        ConversationMessageRole role,
        String content,
        boolean grounded,
        List<KnowledgeCitationResponse> citations,
        Instant createdAt
) {

    public ConversationMessageResponse {
        citations = List.copyOf(citations);
    }
}
