package org.brian.aisupportagent.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.KnowledgeDocumentResponse;
import org.brian.aisupportagent.dto.PagedResponse;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.service.KnowledgeDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminKnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;

    @GetMapping
    public ResponseEntity<PagedResponse<KnowledgeDocumentResponse>> findAll(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size must be 100 or fewer")
            int size
    ) {
        return ResponseEntity.ok(documentService.findAll(page, size));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<KnowledgeDocumentResponse> findById(
            @PathVariable UUID documentId
    ) {
        return ResponseEntity.ok(documentService.findById(documentId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KnowledgeDocumentResponse> upload(
            @RequestParam
            @NotBlank(message = "Display name is required")
            @Size(max = 255, message = "Display name must be 255 characters or fewer")
            String displayName,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        KnowledgeDocumentResponse response = documentService.upload(
                displayName,
                file,
                authenticatedUser
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{documentId}/process")
    public ResponseEntity<KnowledgeDocumentResponse> process(
            @PathVariable UUID documentId
    ) {
        return ResponseEntity.ok(documentService.process(documentId));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId) {
        documentService.delete(documentId);
        return ResponseEntity.noContent().build();
    }
}
