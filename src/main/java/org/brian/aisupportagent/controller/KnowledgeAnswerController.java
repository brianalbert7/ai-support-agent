package org.brian.aisupportagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.KnowledgeAnswerResponse;
import org.brian.aisupportagent.dto.KnowledgeSearchRequest;
import org.brian.aisupportagent.service.KnowledgeAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.brian.aisupportagent.config.OpenApiConfig.BEARER_AUTH_SCHEME;

@RestController
@RequestMapping("/api/knowledge/ask")
@RequiredArgsConstructor
@Tag(name = "Knowledge", description = "Search the knowledge base and generate grounded answers")
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
public class KnowledgeAnswerController {

    private final KnowledgeAnswerService knowledgeAnswerService;

    @PostMapping
    @Operation(summary = "Generate a grounded answer")
    public ResponseEntity<KnowledgeAnswerResponse> answer(
            @Valid @RequestBody KnowledgeSearchRequest request
    ) {
        return ResponseEntity.ok(knowledgeAnswerService.answer(request));
    }
}
