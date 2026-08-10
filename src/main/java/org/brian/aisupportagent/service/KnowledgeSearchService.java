package org.brian.aisupportagent.service;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.config.KnowledgeSearchProperties;
import org.brian.aisupportagent.dto.KnowledgeSearchRequest;
import org.brian.aisupportagent.dto.KnowledgeSearchResponse;
import org.brian.aisupportagent.dto.KnowledgeSearchResultResponse;
import org.brian.aisupportagent.exception.EmbeddingGenerationException;
import org.brian.aisupportagent.exception.KnowledgeSearchException;
import org.brian.aisupportagent.repository.ChunkEmbeddingRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeSearchService {

    private final DocumentEmbeddingService documentEmbeddingService;
    private final ChunkEmbeddingRepository chunkEmbeddingRepository;
    private final KnowledgeSearchProperties properties;

    @PreAuthorize("isAuthenticated()")
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        return search(request, request.question());
    }

    @PreAuthorize("isAuthenticated()")
    public KnowledgeSearchResponse search(
            KnowledgeSearchRequest request,
            String retrievalQuery
    ) {
        String normalizedQuestion = request.question().trim();
        String normalizedRetrievalQuery = retrievalQuery.trim();
        int resultLimit = request.maxResults() == null
                ? properties.defaultResults()
                : request.maxResults();

        try {
            float[] questionEmbedding = documentEmbeddingService.embedQuery(
                    normalizedRetrievalQuery
            );
            List<KnowledgeSearchResultResponse> results = chunkEmbeddingRepository
                    .findSimilar(
                            questionEmbedding,
                            properties.minimumSimilarity(),
                            resultLimit
                    )
                    .stream()
                    .map(this::toResponse)
                    .toList();

            return new KnowledgeSearchResponse(normalizedQuestion, results);
        } catch (EmbeddingGenerationException | DataAccessException exception) {
            throw new KnowledgeSearchException(exception);
        }
    }

    private KnowledgeSearchResultResponse toResponse(RetrievedDocumentChunk chunk) {
        return new KnowledgeSearchResultResponse(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.documentName(),
                chunk.pageNumber(),
                chunk.chunkIndex(),
                chunk.content(),
                chunk.similarity()
        );
    }
}
