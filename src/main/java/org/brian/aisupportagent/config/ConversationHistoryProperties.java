package org.brian.aisupportagent.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.conversation.history")
public record ConversationHistoryProperties(
        @Min(1) @Max(50) int maxMessages,
        @Min(1000) @Max(50000) int maxCharacters
) {
}
