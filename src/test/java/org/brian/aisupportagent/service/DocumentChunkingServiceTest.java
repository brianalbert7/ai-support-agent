package org.brian.aisupportagent.service;

import org.brian.aisupportagent.config.DocumentChunkingProperties;
import org.brian.aisupportagent.exception.DocumentChunkingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentChunkingServiceTest {

    private final DocumentChunkingService chunkingService =
            new DocumentChunkingService(new DocumentChunkingProperties(
                    50,
                    0,
                    1,
                    100
            ));

    @Test
    void splitsLongPageIntoOrderedNonBlankChunks() {
        String pageContent = IntStream.rangeClosed(1, 30)
                .mapToObj(number -> "Policy sentence " + number
                        + " explains an important employee benefit.")
                .collect(Collectors.joining(" "));

        List<String> chunks = chunkingService.chunk(pageContent);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.getFirst().startsWith("Policy sentence 1"));
        assertTrue(chunks.getLast().contains("employee benefit"));
        assertFalse(chunks.stream().anyMatch(String::isBlank));
    }

    @Test
    void keepsShortPageAsSingleChunk() {
        List<String> chunks = chunkingService.chunk(
                "Employees receive twenty vacation days."
        );

        assertEquals(List.of("Employees receive twenty vacation days."), chunks);
    }

    @Test
    void rejectsBlankPageContent() {
        assertThrows(
                DocumentChunkingException.class,
                () -> chunkingService.chunk("  ")
        );
    }
}
