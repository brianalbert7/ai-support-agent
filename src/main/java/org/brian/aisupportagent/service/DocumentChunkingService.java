package org.brian.aisupportagent.service;

import org.brian.aisupportagent.config.DocumentChunkingProperties;
import org.brian.aisupportagent.exception.DocumentChunkingException;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DocumentChunkingService {

    private final TokenTextSplitter textSplitter;

    public DocumentChunkingService(DocumentChunkingProperties properties) {
        this.textSplitter = TokenTextSplitter.builder()
                .withChunkSize(properties.targetTokens())
                .withMinChunkSizeChars(properties.minimumSizeCharacters())
                .withMinChunkLengthToEmbed(properties.minimumLengthToEmbed())
                .withMaxNumChunks(properties.maximumChunksPerPage())
                .withKeepSeparator(true)
                .build();
    }

    public List<String> chunk(String pageContent) {
        if (pageContent == null || pageContent.isBlank()) {
            throw new DocumentChunkingException("Page content must not be blank");
        }

        try {
            return textSplitter.split(new Document(pageContent)).stream()
                    .map(Document::getText).filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(content -> !content.isBlank())
                    .toList();
        } catch (RuntimeException exception) {
            throw new DocumentChunkingException(
                    "Could not split the extracted page text",
                    exception
            );
        }
    }
}
