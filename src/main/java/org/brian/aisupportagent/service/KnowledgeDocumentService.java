package org.brian.aisupportagent.service;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.KnowledgeDocumentResponse;
import org.brian.aisupportagent.dto.PagedResponse;
import org.brian.aisupportagent.entity.DocumentStatus;
import org.brian.aisupportagent.entity.KnowledgeDocumentChunk;
import org.brian.aisupportagent.entity.KnowledgeDocument;
import org.brian.aisupportagent.entity.KnowledgeDocumentPage;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.DocumentChunkingException;
import org.brian.aisupportagent.exception.DocumentDeletionException;
import org.brian.aisupportagent.exception.DocumentProcessingException;
import org.brian.aisupportagent.exception.DocumentStorageException;
import org.brian.aisupportagent.exception.DuplicateDocumentException;
import org.brian.aisupportagent.exception.EmbeddingGenerationException;
import org.brian.aisupportagent.exception.InvalidDocumentStateException;
import org.brian.aisupportagent.exception.KnowledgeDocumentNotFoundException;
import org.brian.aisupportagent.exception.PdfExtractionException;
import org.brian.aisupportagent.repository.ChunkEmbeddingRepository;
import org.brian.aisupportagent.repository.KnowledgeDocumentChunkRepository;
import org.brian.aisupportagent.repository.KnowledgeDocumentPageRepository;
import org.brian.aisupportagent.repository.KnowledgeDocumentRepository;
import org.brian.aisupportagent.util.DocumentFileValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDocumentPageRepository documentPageRepository;
    private final KnowledgeDocumentChunkRepository documentChunkRepository;
    private final ChunkEmbeddingRepository chunkEmbeddingRepository;
    private final DocumentStorageService storageService;
    private final DocumentFileValidator fileValidator;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentEmbeddingService documentEmbeddingService;

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

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(noRollbackFor = DocumentProcessingException.class)
    public KnowledgeDocumentResponse process(UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new KnowledgeDocumentNotFoundException(documentId));
        validateProcessableStatus(document.getStatus());

        document.setStatus(DocumentStatus.PROCESSING);
        document.setFailureReason(null);
        document.setPageCount(null);
        documentRepository.saveAndFlush(document);

        PdfExtractionResult extractionResult;
        List<ChunkedPage> chunkedPages;
        List<KnowledgeDocumentPage> pages;
        List<KnowledgeDocumentChunk> chunks;
        List<ChunkEmbedding> embeddings;
        try {
            extractionResult = pdfTextExtractionService.extract(
                    storageService.load(document.getStorageKey())
            );
            chunkedPages = chunkPages(extractionResult.pages());
            pages = toPageEntities(document, chunkedPages);
            chunks = toChunkEntities(pages, chunkedPages);
            embeddings = documentEmbeddingService.embed(chunks);
        } catch (DocumentStorageException
                 | PdfExtractionException
                 | DocumentChunkingException
                 | EmbeddingGenerationException exception) {
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason(processingFailureReason(exception));
            documentRepository.saveAndFlush(document);
            throw new DocumentProcessingException(exception);
        }

        documentPageRepository.deleteAllByKnowledgeDocumentId(documentId);
        documentPageRepository.saveAllAndFlush(pages);
        documentChunkRepository.saveAllAndFlush(chunks);
        chunkEmbeddingRepository.saveAll(embeddings);

        document.setPageCount(extractionResult.pageCount());
        document.setStatus(DocumentStatus.READY);
        document.setFailureReason(null);
        return toResponse(documentRepository.saveAndFlush(document));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PagedResponse<KnowledgeDocumentResponse> findAll(int page, int size) {
        Page<KnowledgeDocument> documentPage = documentRepository
                .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size));
        return new PagedResponse<>(
                documentPage.getContent().stream().map(this::toResponse).toList(),
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages(),
                documentPage.isFirst(),
                documentPage.isLast()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse findById(UUID documentId) {
        return toResponse(findDocument(documentId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(UUID documentId) {
        KnowledgeDocument document = findDocument(documentId);
        String storageKey = document.getStorageKey();
        documentRepository.delete(document);
        documentRepository.flush();

        try {
            storageService.delete(storageKey);
        } catch (DocumentStorageException exception) {
            throw new DocumentDeletionException(exception);
        }
    }

    private KnowledgeDocument findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new KnowledgeDocumentNotFoundException(documentId));
    }

    private void validateProcessableStatus(DocumentStatus status) {
        if (status != DocumentStatus.UPLOADED && status != DocumentStatus.FAILED) {
            throw new InvalidDocumentStateException(status);
        }
    }

    private List<ChunkedPage> chunkPages(List<ExtractedDocumentPage> pages) {
        List<ChunkedPage> chunkedPages = pages.stream()
                .map(page -> new ChunkedPage(
                        page,
                        documentChunkingService.chunk(page.content())
                ))
                .toList();

        boolean noChunksCreated = chunkedPages.stream()
                .allMatch(page -> page.chunks().isEmpty());
        if (noChunksCreated) {
            throw new DocumentChunkingException(
                    "The extracted text was too short to create searchable chunks"
            );
        }

        return chunkedPages;
    }

    private List<KnowledgeDocumentPage> toPageEntities(
            KnowledgeDocument document,
            List<ChunkedPage> chunkedPages
    ) {
        return chunkedPages.stream()
                .map(chunkedPage -> KnowledgeDocumentPage.builder()
                        .knowledgeDocument(document)
                        .pageNumber(chunkedPage.page().pageNumber())
                        .content(chunkedPage.page().content())
                        .build())
                .toList();
    }

    private List<KnowledgeDocumentChunk> toChunkEntities(
            List<KnowledgeDocumentPage> pages,
            List<ChunkedPage> chunkedPages
    ) {
        Map<Integer, KnowledgeDocumentPage> pagesByNumber = pages.stream()
                .collect(Collectors.toMap(
                        KnowledgeDocumentPage::getPageNumber,
                        Function.identity()
                ));
        List<KnowledgeDocumentChunk> chunks = new ArrayList<>();

        for (ChunkedPage chunkedPage : chunkedPages) {
            KnowledgeDocumentPage page = pagesByNumber.get(
                    chunkedPage.page().pageNumber()
            );
            for (int chunkIndex = 0;
                 chunkIndex < chunkedPage.chunks().size();
                 chunkIndex++) {
                chunks.add(KnowledgeDocumentChunk.builder()
                        .knowledgeDocumentPage(page)
                        .chunkIndex(chunkIndex)
                        .content(chunkedPage.chunks().get(chunkIndex))
                        .build());
            }
        }

        return chunks;
    }

    private String processingFailureReason(RuntimeException exception) {
        String message = "Document processing failed: " + exception.getMessage();
        return message.substring(0, Math.min(message.length(), 1000));
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
                .failureReason(document.getFailureReason())
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

    private record ChunkedPage(
            ExtractedDocumentPage page,
            List<String> chunks
    ) {

        private ChunkedPage {
            chunks = List.copyOf(chunks);
        }
    }
}
