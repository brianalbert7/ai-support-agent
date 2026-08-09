package org.brian.aisupportagent.service;

public record StoredDocumentFile(
        String storageKey,
        String checksumSha256,
        long sizeBytes
) {
}
