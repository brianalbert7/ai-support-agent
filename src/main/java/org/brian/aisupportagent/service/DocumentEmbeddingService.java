package org.brian.aisupportagent.service;

import org.brian.aisupportagent.config.DocumentEmbeddingProperties;
import org.brian.aisupportagent.entity.KnowledgeDocumentChunk;
import org.brian.aisupportagent.exception.EmbeddingGenerationException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final int dimensions;
    private final int batchSize;

    public DocumentEmbeddingService(
            EmbeddingModel embeddingModel,
            DocumentEmbeddingProperties properties
    ) {
        this.embeddingModel = embeddingModel;
        this.dimensions = properties.dimensions();
        this.batchSize = properties.batchSize();
    }

    public List<ChunkEmbedding> embed(List<KnowledgeDocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            throw new EmbeddingGenerationException("No document chunks were provided");
        }

        List<ChunkEmbedding> chunkEmbeddings = new ArrayList<>(chunks.size());
        try {
            for (int start = 0; start < chunks.size(); start += batchSize) {
                int end = Math.min(start + batchSize, chunks.size());
                List<KnowledgeDocumentChunk> batch = chunks.subList(start, end);
                List<float[]> vectors = embeddingModel.embed(
                        batch.stream().map(KnowledgeDocumentChunk::getContent).toList()
                );
                validateBatch(batch, vectors);

                for (int index = 0; index < batch.size(); index++) {
                    chunkEmbeddings.add(new ChunkEmbedding(
                            batch.get(index),
                            vectors.get(index)
                    ));
                }
            }
        } catch (EmbeddingGenerationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new EmbeddingGenerationException(
                    "Could not generate document chunk embeddings",
                    exception
            );
        }

        return List.copyOf(chunkEmbeddings);
    }

    private void validateBatch(
            List<KnowledgeDocumentChunk> chunks,
            List<float[]> vectors
    ) {
        if (vectors == null || vectors.size() != chunks.size()) {
            throw new EmbeddingGenerationException(
                    "The embedding model returned an unexpected number of vectors"
            );
        }

        for (float[] vector : vectors) {
            if (vector == null || vector.length != dimensions) {
                throw new EmbeddingGenerationException(
                        "The embedding model returned an unexpected vector dimension"
                );
            }
            for (float value : vector) {
                if (!Float.isFinite(value)) {
                    throw new EmbeddingGenerationException(
                            "The embedding model returned a non-finite vector value"
                    );
                }
            }
        }
    }
}
