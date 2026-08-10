package org.brian.aisupportagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(
        @NotBlank(message = "Conversation title is required")
        @Size(max = 200, message = "Conversation title must be 200 characters or fewer")
        String title
) {
}
