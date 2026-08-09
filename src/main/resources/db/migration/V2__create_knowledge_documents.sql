CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    page_count INTEGER,
    failure_reason VARCHAR(1000),
    uploaded_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_knowledge_documents_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_knowledge_documents_size CHECK (size_bytes > 0),
    CONSTRAINT ck_knowledge_documents_checksum
        CHECK (checksum_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_knowledge_documents_status
        CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT ck_knowledge_documents_page_count
        CHECK (page_count IS NULL OR page_count > 0),
    CONSTRAINT fk_knowledge_documents_uploader
        FOREIGN KEY (uploaded_by_user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_knowledge_documents_status
    ON knowledge_documents (status);

CREATE INDEX idx_knowledge_documents_uploaded_by
    ON knowledge_documents (uploaded_by_user_id);

CREATE INDEX idx_knowledge_documents_created_at
    ON knowledge_documents (created_at DESC);
