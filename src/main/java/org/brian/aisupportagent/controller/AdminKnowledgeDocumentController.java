package org.brian.aisupportagent.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.KnowledgeDocumentResponse;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.service.KnowledgeDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminKnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;

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
}
