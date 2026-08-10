CREATE TABLE conversation_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    grounded BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_conversation_messages_role
        CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_conversation_messages_content
        CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT fk_conversation_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES conversations (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_conversation_messages_conversation_created
    ON conversation_messages (conversation_id, created_at, id);

CREATE TABLE conversation_message_citations (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    source_number INTEGER NOT NULL,
    chunk_id UUID NOT NULL,
    document_id UUID NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    page_number INTEGER NOT NULL,
    excerpt TEXT NOT NULL,
    similarity DOUBLE PRECISION NOT NULL,

    CONSTRAINT uk_conversation_message_citations_message_source
        UNIQUE (message_id, source_number),
    CONSTRAINT ck_conversation_message_citations_source_number
        CHECK (source_number > 0),
    CONSTRAINT ck_conversation_message_citations_page_number
        CHECK (page_number > 0),
    CONSTRAINT ck_conversation_message_citations_excerpt
        CHECK (LENGTH(TRIM(excerpt)) > 0),
    CONSTRAINT fk_conversation_message_citations_message
        FOREIGN KEY (message_id)
        REFERENCES conversation_messages (id)
        ON DELETE CASCADE
);
