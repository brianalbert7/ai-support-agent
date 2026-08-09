package org.brian.aisupportagent.service;

import org.brian.aisupportagent.config.DocumentStorageProperties;
import org.brian.aisupportagent.exception.DocumentStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalDocumentStorageServiceTest {

    @TempDir
    Path storageDirectory;

    @Test
    void storesFileWithGeneratedKeyAndSha256Checksum() throws Exception {
        LocalDocumentStorageService storageService = storageService();
        byte[] content = "%PDF-1.4\ntest document".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "handbook.pdf",
                "application/pdf",
                content
        );

        StoredDocumentFile storedFile = storageService.store(file);
        Path storedPath = storageDirectory.resolve(storedFile.storageKey());

        assertTrue(storedFile.storageKey().endsWith(".pdf"));
        assertEquals(content.length, storedFile.sizeBytes());
        assertEquals(sha256(content), storedFile.checksumSha256());
        assertArrayEquals(content, Files.readAllBytes(storedPath));

        storageService.delete(storedFile.storageKey());
        assertFalse(Files.exists(storedPath));
    }

    @Test
    void rejectsStorageKeysThatEscapeConfiguredDirectory() {
        LocalDocumentStorageService storageService = storageService();

        assertThrows(
                DocumentStorageException.class,
                () -> storageService.delete("../outside.pdf")
        );
    }

    private LocalDocumentStorageService storageService() {
        DocumentStorageProperties properties = new DocumentStorageProperties(
                storageDirectory,
                DataSize.ofMegabytes(10)
        );
        return new LocalDocumentStorageService(properties);
    }

    private String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
        );
    }
}
