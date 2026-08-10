package org.brian.aisupportagent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.AskConversationRequest;
import org.brian.aisupportagent.dto.ConversationExchangeResponse;
import org.brian.aisupportagent.dto.ConversationMessageResponse;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.service.ConversationMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class ConversationMessageController {

    private final ConversationMessageService conversationMessageService;

    @PostMapping
    public ResponseEntity<ConversationExchangeResponse> ask(
            @PathVariable UUID conversationId,
            @Valid @RequestBody AskConversationRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        ConversationExchangeResponse response = conversationMessageService.ask(
                conversationId,
                request,
                authenticatedUser
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversationMessageResponse>> findHistory(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        return ResponseEntity.ok(conversationMessageService.findHistory(
                conversationId,
                authenticatedUser
        ));
    }
}
