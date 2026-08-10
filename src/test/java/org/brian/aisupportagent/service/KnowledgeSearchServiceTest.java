package org.brian.aisupportagent.service;

import org.brian.aisupportagent.config.KnowledgeSearchProperties;
import org.brian.aisupportagent.dto.KnowledgeSearchRequest;
import org.brian.aisupportagent.dto.KnowledgeSearchResponse;
import org.brian.aisupportagent.exception.EmbeddingGenerationException;
import org.brian.aisupportagent.exception.KnowledgeSearchException;
import org.brian.aisupportagent.repository.ChunkEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeSearchServiceTest {

    @Mock
    private DocumentEmbeddingService documentEmbeddingService;

    @Mock
    private ChunkEmbeddingRepository chunkEmbeddingRepository;

    private KnowledgeSearchService knowledgeSearchService;

    @BeforeEach
    void setUp() {
        knowledgeSearchService = new KnowledgeSearchService(
                documentEmbeddingService,
                chunkEmbeddingRepository,
                new KnowledgeSearchProperties(5, 0.70)
        );
    }

    @Test
    void normalizesQuestionUsesDefaultLimitAndMapsRankedChunks() {
        float[] questionEmbedding = new float[]{1.0f, 0.0f, 0.0f};
        RetrievedDocumentChunk retrievedChunk = new RetrievedDocumentChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Employee Handbook",
                7,
                1,
                "Employees receive twenty vacation days each year.",
                0.92
        );
        when(documentEmbeddingService.embedQuery("How many vacation days do I get?"))
                .thenReturn(questionEmbedding);
        when(chunkEmbeddingRepository.findSimilar(questionEmbedding, 0.70, 5))
                .thenReturn(List.of(retrievedChunk));

        KnowledgeSearchResponse response = knowledgeSearchService.search(
                new KnowledgeSearchRequest(
                        "  How many vacation days do I get?  ",
                        null
                )
        );

        assertEquals("How many vacation days do I get?", response.question());
        assertEquals(1, response.results().size());
        assertEquals("Employee Handbook", response.results().getFirst().documentName());
        assertEquals(7, response.results().getFirst().pageNumber());
        assertEquals(0.92, response.results().getFirst().similarity());
        verify(chunkEmbeddingRepository).findSimilar(questionEmbedding, 0.70, 5);
    }

    @Test
    void honorsValidatedRequestLimit() {
        float[] questionEmbedding = new float[]{1.0f, 0.0f, 0.0f};
        when(documentEmbeddingService.embedQuery("reset password"))
                .thenReturn(questionEmbedding);
        when(chunkEmbeddingRepository.findSimilar(questionEmbedding, 0.70, 2))
                .thenReturn(List.of());

        KnowledgeSearchResponse response = knowledgeSearchService.search(
                new KnowledgeSearchRequest("reset password", 2)
        );

        assertEquals(List.of(), response.results());
        verify(chunkEmbeddingRepository).findSimilar(questionEmbedding, 0.70, 2);
    }

    @Test
    void usesContextualRetrievalQueryButPreservesCurrentQuestion() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest(
                "What about part-time employees?",
                3
        );
        String retrievalQuery = """
                PRIOR USER QUESTIONS:
                - How many vacation days do full-time employees receive?
                CURRENT QUESTION:
                What about part-time employees?
                """.trim();
        float[] questionEmbedding = new float[]{1.0f, 0.0f, 0.0f};
        when(documentEmbeddingService.embedQuery(retrievalQuery))
                .thenReturn(questionEmbedding);
        when(chunkEmbeddingRepository.findSimilar(questionEmbedding, 0.70, 3))
                .thenReturn(List.of());

        KnowledgeSearchResponse response = knowledgeSearchService.search(
                request,
                retrievalQuery
        );

        assertEquals("What about part-time employees?", response.question());
        verify(documentEmbeddingService).embedQuery(retrievalQuery);
    }

    @Test
    void translatesEmbeddingFailureWithoutQueryingDatabase() {
        when(documentEmbeddingService.embedQuery("vacation policy"))
                .thenThrow(new EmbeddingGenerationException("Provider unavailable"));

        assertThrows(
                KnowledgeSearchException.class,
                () -> knowledgeSearchService.search(
                        new KnowledgeSearchRequest("vacation policy", null)
                )
        );

        verifyNoInteractions(chunkEmbeddingRepository);
    }
}
