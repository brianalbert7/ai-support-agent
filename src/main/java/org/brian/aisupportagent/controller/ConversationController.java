package org.brian.aisupportagent.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.ConversationResponse;
import org.brian.aisupportagent.dto.CreateConversationRequest;
import org.brian.aisupportagent.dto.PagedResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<PagedResponse<ConversationResponse>> findAll(
            @AuthenticationPrincipal User authenticatedUser,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size must be 100 or fewer")
            int size
    ) {
        return ResponseEntity.ok(conversationService.findAllFor(
                authenticatedUser,
                page,
                size
        ));
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
