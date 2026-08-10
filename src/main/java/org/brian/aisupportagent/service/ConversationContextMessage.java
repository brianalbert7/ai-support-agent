package org.brian.aisupportagent.service;

import org.brian.aisupportagent.entity.ConversationMessageRole;

public record ConversationContextMessage(
        ConversationMessageRole role,
        String content
) {
}
