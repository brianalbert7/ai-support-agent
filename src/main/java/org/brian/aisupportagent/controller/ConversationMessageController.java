package org.brian.aisupportagent.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.AskConversationRequest;
import org.brian.aisupportagent.dto.ConversationExchangeResponse;
import org.brian.aisupportagent.dto.ConversationMessageResponse;
import org.brian.aisupportagent.dto.PagedResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<PagedResponse<ConversationMessageResponse>> findHistory(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal User authenticatedUser,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size must be 100 or fewer")
            int size
    ) {
        return ResponseEntity.ok(conversationMessageService.findHistory(
                conversationId,
                authenticatedUser,
                page,
                size
        ));
    }
}
