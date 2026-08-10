package org.brian.aisupportagent.controller;

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

@RestController
@RequestMapping("/api/knowledge/ask")
@RequiredArgsConstructor
public class KnowledgeAnswerController {

    private final KnowledgeAnswerService knowledgeAnswerService;

    @PostMapping
    public ResponseEntity<KnowledgeAnswerResponse> answer(
            @Valid @RequestBody KnowledgeSearchRequest request
    ) {
        return ResponseEntity.ok(knowledgeAnswerService.answer(request));
    }
}
