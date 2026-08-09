package org.brian.aisupportagent.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "app.storage.documents")
public record DocumentStorageProperties(
        @NotNull Path directory,
        @NotNull DataSize maxFileSize
) {
    public DocumentStorageProperties {
        if (maxFileSize != null && maxFileSize.toBytes() <= 0) {
            throw new IllegalArgumentException("Document maximum file size must be positive");
        }
    }
}
