package org.brian.aisupportagent.dto;

public record ConversationExchangeResponse(
        ConversationMessageResponse userMessage,
        ConversationMessageResponse assistantMessage
) {
}
