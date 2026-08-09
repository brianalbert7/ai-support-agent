package org.brian.aisupportagent.service;

import org.brian.aisupportagent.config.DocumentStorageProperties;
import org.brian.aisupportagent.exception.DocumentStorageException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class LocalDocumentStorageService implements DocumentStorageService {

    private final Path storageDirectory;

    public LocalDocumentStorageService(DocumentStorageProperties properties) {
        this.storageDirectory = properties.directory().toAbsolutePath().normalize();
    }

    @Override
    public StoredDocumentFile store(MultipartFile file) {
        Path temporaryFile = null;

        try {
            Files.createDirectories(storageDirectory);
            temporaryFile = Files.createTempFile(storageDirectory, "upload-", ".tmp");

            MessageDigest digest = sha256Digest();
            long sizeBytes;
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest);
                 OutputStream output = Files.newOutputStream(temporaryFile)) {
                sizeBytes = input.transferTo(output);
            }

            String storageKey = UUID.randomUUID() + ".pdf";
            Path destination = resolveStorageKey(storageKey);
            moveIntoPlace(temporaryFile, destination);

            return new StoredDocumentFile(
                    storageKey,
                    HexFormat.of().formatHex(digest.digest()),
                    sizeBytes
            );
        } catch (IOException exception) {
            deleteTemporaryFile(temporaryFile);
            throw new DocumentStorageException("Could not store the uploaded document", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveStorageKey(storageKey));
        } catch (IOException exception) {
            throw new DocumentStorageException("Could not delete the stored document", exception);
        }
    }

    private Path resolveStorageKey(String storageKey) {
        Path resolvedPath = storageDirectory.resolve(storageKey).normalize();
        if (!resolvedPath.startsWith(storageDirectory)) {
            throw new DocumentStorageException("Invalid document storage key");
        }
        return resolvedPath;
    }

    private void moveIntoPlace(Path temporaryFile, Path destination) throws IOException {
        try {
            Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, destination);
        }
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException ignored) {
            // The original storage exception remains the primary failure.
        }
    }
}
