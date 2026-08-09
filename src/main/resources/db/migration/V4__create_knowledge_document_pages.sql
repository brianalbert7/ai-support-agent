CREATE TABLE knowledge_document_pages (
    id UUID PRIMARY KEY,
    knowledge_document_id UUID NOT NULL,
    page_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_knowledge_document_pages_document_page
        UNIQUE (knowledge_document_id, page_number),
    CONSTRAINT ck_knowledge_document_pages_page_number
        CHECK (page_number > 0),
    CONSTRAINT fk_knowledge_document_pages_document
        FOREIGN KEY (knowledge_document_id)
        REFERENCES knowledge_documents (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_knowledge_document_pages_document
    ON knowledge_document_pages (knowledge_document_id);
