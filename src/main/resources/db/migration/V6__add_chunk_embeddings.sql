CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE knowledge_document_chunks
    ADD COLUMN embedding vector(1536);

CREATE INDEX idx_knowledge_document_chunks_embedding_hnsw
    ON knowledge_document_chunks
    USING hnsw (embedding vector_cosine_ops);

UPDATE knowledge_documents document
SET status = 'FAILED',
    failure_reason = 'Reprocessing is required to generate vector embeddings'
WHERE document.status = 'READY'
  AND EXISTS (
      SELECT 1
      FROM knowledge_document_pages page
      JOIN knowledge_document_chunks chunk
        ON chunk.knowledge_document_page_id = page.id
      WHERE page.knowledge_document_id = document.id
        AND chunk.embedding IS NULL
  );
