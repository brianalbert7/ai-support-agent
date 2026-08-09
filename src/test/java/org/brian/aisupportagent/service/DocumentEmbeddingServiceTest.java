package org.brian.aisupportagent.service;

import org.brian.aisupportagent.config.DocumentEmbeddingProperties;
import org.brian.aisupportagent.entity.KnowledgeDocumentChunk;
import org.brian.aisupportagent.exception.EmbeddingGenerationException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentEmbeddingServiceTest {

    private StubEmbeddingModel embeddingModel;
    private DocumentEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingModel = new StubEmbeddingModel();
        embeddingService = new DocumentEmbeddingService(
                embeddingModel,
                new DocumentEmbeddingProperties(3, 2)
        );
    }

    @Test
    void embedsChunksInConfiguredBatchesAndPreservesTheirOrder() {
        List<KnowledgeDocumentChunk> chunks = List.of(
                chunk("first"),
                chunk("second"),
                chunk("third")
        );
        embeddingModel.addResponse(List.of(vector(1), vector(2)));
        embeddingModel.addResponse(List.of(vector(3)));

        List<ChunkEmbedding> embeddings = embeddingService.embed(chunks);

        assertEquals(3, embeddings.size());
        assertSame(chunks.getFirst(), embeddings.getFirst().chunk());
        assertEquals(1.0f, embeddings.getFirst().vector()[0]);
        assertSame(chunks.getLast(), embeddings.getLast().chunk());
        assertEquals(3.0f, embeddings.getLast().vector()[0]);
        assertEquals(List.of(2, 1), embeddingModel.requestSizes());
    }

    @Test
    void rejectsUnexpectedEmbeddingCount() {
        embeddingModel.addResponse(List.of(vector(1)));

        assertThrows(
                EmbeddingGenerationException.class,
                () -> embeddingService.embed(List.of(chunk("first"), chunk("second")))
        );
    }

    @Test
    void rejectsUnexpectedEmbeddingDimensionAndNonFiniteValues() {
        embeddingModel.addResponse(List.of(new float[]{1.0f, 2.0f}));
        assertThrows(
                EmbeddingGenerationException.class,
                () -> embeddingService.embed(List.of(chunk("wrong dimensions")))
        );

        embeddingModel.addResponse(List.of(new float[]{1.0f, Float.NaN, 3.0f}));
        assertThrows(
                EmbeddingGenerationException.class,
                () -> embeddingService.embed(List.of(chunk("not finite")))
        );
    }

    @Test
    void rejectsEmptyChunkListWithoutCallingModel() {
        assertThrows(
                EmbeddingGenerationException.class,
                () -> embeddingService.embed(List.of())
        );

        assertEquals(List.of(), embeddingModel.requestSizes());
    }

    private KnowledgeDocumentChunk chunk(String content) {
        return KnowledgeDocumentChunk.builder().content(content).build();
    }

    private float[] vector(float firstValue) {
        return new float[]{firstValue, 0.25f, 0.5f};
    }

    private static final class StubEmbeddingModel implements EmbeddingModel {

        private final Deque<List<float[]>> responses = new ArrayDeque<>();
        private final List<Integer> requestSizes = new ArrayList<>();

        private void addResponse(List<float[]> response) {
            responses.addLast(response);
        }

        private List<Integer> requestSizes() {
            return List.copyOf(requestSizes);
        }

        @Override
        public @NonNull EmbeddingResponse call(EmbeddingRequest request) {
            requestSizes.add(request.getInstructions().size());
            List<float[]> vectors = responses.removeFirst();
            List<Embedding> results = IntStream.range(0, vectors.size())
                    .mapToObj(index -> new Embedding(vectors.get(index), index))
                    .toList();
            return new EmbeddingResponse(results);
        }

        @Override
        public float @NonNull [] embed(Document document) {
            assert document.getText() != null;
            return embed(document.getText());
        }
    }
}
