ALTER TABLE knowledge_documents
    ADD CONSTRAINT uk_knowledge_documents_checksum UNIQUE (checksum_sha256);
