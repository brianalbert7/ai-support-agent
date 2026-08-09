package org.brian.aisupportagent.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rag.embedding")
public record DocumentEmbeddingProperties(
        @Min(1) int dimensions,
        @Min(1) @Max(1000) int batchSize
) {
}
