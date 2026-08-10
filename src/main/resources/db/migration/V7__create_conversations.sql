CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_conversations_title
        CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT fk_conversations_owner
        FOREIGN KEY (owner_user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_conversations_owner_updated
    ON conversations (owner_user_id, updated_at DESC);
