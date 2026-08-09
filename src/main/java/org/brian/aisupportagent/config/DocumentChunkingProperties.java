package org.brian.aisupportagent.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rag.chunking")
public record DocumentChunkingProperties(
        @Min(50) @Max(2000) int targetTokens,
        @Min(0) int minimumSizeCharacters,
        @Min(1) int minimumLengthToEmbed,
        @Min(1) @Max(1000) int maximumChunksPerPage
) {
}
