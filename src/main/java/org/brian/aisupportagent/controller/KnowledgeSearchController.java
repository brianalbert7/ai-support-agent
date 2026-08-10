package org.brian.aisupportagent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.KnowledgeSearchRequest;
import org.brian.aisupportagent.dto.KnowledgeSearchResponse;
import org.brian.aisupportagent.service.KnowledgeSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge/search")
@RequiredArgsConstructor
public class KnowledgeSearchController {

    private final KnowledgeSearchService knowledgeSearchService;

    @PostMapping
    public ResponseEntity<KnowledgeSearchResponse> search(
            @Valid @RequestBody KnowledgeSearchRequest request
    ) {
        return ResponseEntity.ok(knowledgeSearchService.search(request));
    }
}
