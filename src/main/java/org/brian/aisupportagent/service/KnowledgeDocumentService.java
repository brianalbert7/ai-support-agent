package org.brian.aisupportagent.service;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.KnowledgeDocumentResponse;
import org.brian.aisupportagent.entity.DocumentStatus;
import org.brian.aisupportagent.entity.KnowledgeDocument;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.DocumentStorageException;
import org.brian.aisupportagent.exception.DuplicateDocumentException;
import org.brian.aisupportagent.repository.KnowledgeDocumentRepository;
import org.brian.aisupportagent.util.DocumentFileValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final DocumentFileValidator fileValidator;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public KnowledgeDocumentResponse upload(
            String displayName,
            MultipartFile file,
            User uploadedBy
    ) {
        String originalFilename = fileValidator.validateAndGetOriginalFilename(file);
        StoredDocumentFile storedFile = storageService.store(file);

        try {
            if (documentRepository.existsByChecksumSha256(storedFile.checksumSha256())) {
                throw new DuplicateDocumentException();
            }

            KnowledgeDocument document = KnowledgeDocument.builder()
                    .displayName(displayName.trim())
                    .originalFileName(originalFilename)
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .sizeBytes(storedFile.sizeBytes())
                    .storageKey(storedFile.storageKey())
                    .checksumSha256(storedFile.checksumSha256())
                    .status(DocumentStatus.UPLOADED)
                    .uploadedBy(uploadedBy)
                    .build();

            return toResponse(documentRepository.saveAndFlush(document));
        } catch (DataIntegrityViolationException exception) {
            cleanupStoredFile(storedFile.storageKey(), exception);
            throw new DuplicateDocumentException();
        } catch (RuntimeException exception) {
            cleanupStoredFile(storedFile.storageKey(), exception);
            throw exception;
        }
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocument document) {
        return KnowledgeDocumentResponse.builder()
                .id(document.getId())
                .displayName(document.getDisplayName())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .sizeBytes(document.getSizeBytes())
                .status(document.getStatus())
                .pageCount(document.getPageCount())
                .uploadedByUserId(document.getUploadedBy().getId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private void cleanupStoredFile(String storageKey, RuntimeException originalException) {
        try {
            storageService.delete(storageKey);
        } catch (DocumentStorageException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }
}
