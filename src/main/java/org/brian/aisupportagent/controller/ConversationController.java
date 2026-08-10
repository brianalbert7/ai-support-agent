package org.brian.aisupportagent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.ConversationResponse;
import org.brian.aisupportagent.dto.CreateConversationRequest;
import org.brian.aisupportagent.dto.UpdateConversationRequest;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.service.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationResponse> create(
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        ConversationResponse response = conversationService.create(
                request,
                authenticatedUser
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> findAll(
            @AuthenticationPrincipal User authenticatedUser
    ) {
        return ResponseEntity.ok(conversationService.findAllFor(authenticatedUser));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> findById(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        return ResponseEntity.ok(conversationService.findOwned(
                conversationId,
                authenticatedUser
        ));
    }

    @PatchMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> update(
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateConversationRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        return ResponseEntity.ok(conversationService.update(
                conversationId,
                request,
                authenticatedUser
        ));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        conversationService.delete(conversationId, authenticatedUser);
        return ResponseEntity.noContent().build();
    }
}
