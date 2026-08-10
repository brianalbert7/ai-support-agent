package org.brian.aisupportagent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeSearchRequest(
        @NotBlank(message = "Question is required")
        @Size(max = 2000, message = "Question must be 2000 characters or fewer")
        String question,

        @Min(value = 1, message = "Maximum results must be at least 1")
        @Max(value = 20, message = "Maximum results must be 20 or fewer")
        Integer maxResults
) {
}
