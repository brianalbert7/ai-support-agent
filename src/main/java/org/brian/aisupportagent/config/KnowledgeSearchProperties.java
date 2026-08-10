package org.brian.aisupportagent.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rag.search")
public record KnowledgeSearchProperties(
        @Min(1) @Max(20) int defaultResults,
        @DecimalMin("0.0") @DecimalMax("1.0") double minimumSimilarity
) {
}
