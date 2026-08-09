package org.brian.aisupportagent.service;

import org.brian.aisupportagent.entity.KnowledgeDocumentChunk;

public record ChunkEmbedding(
        KnowledgeDocumentChunk chunk,
        float[] vector
) {

    public ChunkEmbedding {
        vector = vector.clone();
    }

    @Override
    public float[] vector() {
        return vector.clone();
    }
}
