CREATE TABLE knowledge_document_chunks (
    id UUID PRIMARY KEY,
    knowledge_document_page_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_knowledge_document_chunks_page_index
        UNIQUE (knowledge_document_page_id, chunk_index),
    CONSTRAINT ck_knowledge_document_chunks_index
        CHECK (chunk_index >= 0),
    CONSTRAINT ck_knowledge_document_chunks_content
        CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT fk_knowledge_document_chunks_page
        FOREIGN KEY (knowledge_document_page_id)
        REFERENCES knowledge_document_pages (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_knowledge_document_chunks_page
    ON knowledge_document_chunks (knowledge_document_page_id);
