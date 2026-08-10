package org.brian.aisupportagent.service;

import org.brian.aisupportagent.dto.KnowledgeDocumentResponse;
import org.brian.aisupportagent.dto.PagedResponse;
import org.brian.aisupportagent.entity.DocumentStatus;
import org.brian.aisupportagent.entity.KnowledgeDocument;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.DocumentDeletionException;
import org.brian.aisupportagent.exception.DocumentStorageException;
import org.brian.aisupportagent.exception.KnowledgeDocumentNotFoundException;
import org.brian.aisupportagent.repository.ChunkEmbeddingRepository;
import org.brian.aisupportagent.repository.KnowledgeDocumentChunkRepository;
import org.brian.aisupportagent.repository.KnowledgeDocumentPageRepository;
import org.brian.aisupportagent.repository.KnowledgeDocumentRepository;
import org.brian.aisupportagent.util.DocumentFileValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentServiceTest {

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private KnowledgeDocumentPageRepository documentPageRepository;

    @Mock
    private KnowledgeDocumentChunkRepository documentChunkRepository;

    @Mock
    private ChunkEmbeddingRepository chunkEmbeddingRepository;

    @Mock
    private DocumentStorageService storageService;

    @Mock
    private DocumentFileValidator fileValidator;

    @Mock
    private PdfTextExtractionService pdfTextExtractionService;

    @Mock
    private DocumentChunkingService documentChunkingService;

    @Mock
    private DocumentEmbeddingService documentEmbeddingService;

    @InjectMocks
    private KnowledgeDocumentService documentService;

    @Test
    void returnsRequestedDocumentPageWithAdminMetadata() {
        PageRequest pageRequest = PageRequest.of(1, 2);
        KnowledgeDocument document = document(DocumentStatus.FAILED);
        when(documentRepository.findAllByOrderByCreatedAtDescIdDesc(pageRequest))
                .thenReturn(new PageImpl<>(List.of(document), pageRequest, 3));

        PagedResponse<KnowledgeDocumentResponse> response = documentService.findAll(1, 2);

        assertEquals(1, response.content().size());
        assertEquals(document.getId(), response.content().getFirst().id());
        assertEquals("Employee Handbook", response.content().getFirst().displayName());
        assertEquals("Extraction failed", response.content().getFirst().failureReason());
        assertEquals(document.getUploadedBy().getId(),
                response.content().getFirst().uploadedByUserId());
        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(3, response.totalElements());
        assertEquals(2, response.totalPages());
        assertFalse(response.first());
        assertTrue(response.last());
    }

    @Test
    void returnsDocumentDetailById() {
        KnowledgeDocument document = document(DocumentStatus.READY);
        when(documentRepository.findById(document.getId()))
                .thenReturn(Optional.of(document));

        KnowledgeDocumentResponse response = documentService.findById(document.getId());

        assertEquals(document.getId(), response.id());
        assertEquals(DocumentStatus.READY, response.status());
        assertEquals(3, response.pageCount());
    }

    @Test
    void rejectsUnknownDocumentDetail() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThrows(
                KnowledgeDocumentNotFoundException.class,
                () -> documentService.findById(documentId)
        );
    }

    @Test
    void flushesDatabaseDeleteBeforeRemovingStoredFile() {
        KnowledgeDocument document = document(DocumentStatus.READY);
        when(documentRepository.findById(document.getId()))
                .thenReturn(Optional.of(document));

        documentService.delete(document.getId());

        InOrder deletionOrder = inOrder(documentRepository, storageService);
        deletionOrder.verify(documentRepository).delete(document);
        deletionOrder.verify(documentRepository).flush();
        deletionOrder.verify(storageService).delete(document.getStorageKey());
    }

    @Test
    void wrapsStorageFailureSoDatabaseTransactionCanRollBack() {
        KnowledgeDocument document = document(DocumentStatus.READY);
        when(documentRepository.findById(document.getId()))
                .thenReturn(Optional.of(document));
        org.mockito.Mockito.doThrow(new DocumentStorageException("Disk unavailable"))
                .when(storageService)
                .delete(document.getStorageKey());

        assertThrows(
                DocumentDeletionException.class,
                () -> documentService.delete(document.getId())
        );

        verify(documentRepository).delete(document);
        verify(documentRepository).flush();
    }

    @Test
    void rejectsUnknownDocumentDeleteWithoutTouchingStorage() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThrows(
                KnowledgeDocumentNotFoundException.class,
                () -> documentService.delete(documentId)
        );

        verify(documentRepository, never()).delete(
                org.mockito.ArgumentMatchers.any(KnowledgeDocument.class)
        );
        verify(storageService, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    private KnowledgeDocument document(DocumentStatus status) {
        return KnowledgeDocument.builder()
                .id(UUID.randomUUID())
                .displayName("Employee Handbook")
                .originalFileName("employee-handbook.pdf")
                .contentType("application/pdf")
                .sizeBytes(1024)
                .storageKey(UUID.randomUUID() + ".pdf")
                .checksumSha256("a".repeat(64))
                .status(status)
                .pageCount(status == DocumentStatus.READY ? 3 : null)
                .failureReason(status == DocumentStatus.FAILED
                        ? "Extraction failed"
                        : null)
                .uploadedBy(User.builder().id(UUID.randomUUID()).build())
                .createdAt(Instant.parse("2026-08-09T12:00:00Z"))
                .updatedAt(Instant.parse("2026-08-09T13:00:00Z"))
                .build();
    }
}
