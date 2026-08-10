package org.brian.aisupportagent.service;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.AskConversationRequest;
import org.brian.aisupportagent.dto.ConversationExchangeResponse;
import org.brian.aisupportagent.dto.ConversationMessageResponse;
import org.brian.aisupportagent.dto.KnowledgeAnswerResponse;
import org.brian.aisupportagent.dto.KnowledgeSearchRequest;
import org.brian.aisupportagent.dto.PagedResponse;
import org.brian.aisupportagent.entity.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationMessageService {

    private final ConversationMessagePersistenceService persistenceService;
    private final KnowledgeAnswerService knowledgeAnswerService;

    @PreAuthorize("isAuthenticated()")
    public ConversationExchangeResponse ask(
            UUID conversationId,
            AskConversationRequest request,
            User authenticatedUser
    ) {
        String question = request.question().trim();
        ConversationMessageResponse userMessage = persistenceService.saveUserMessage(
                conversationId,
                authenticatedUser.getId(),
                question
        );
        List<ConversationContextMessage> history = persistenceService.findRecentContext(
                conversationId,
                authenticatedUser.getId(),
                userMessage.id()
        );
        KnowledgeAnswerResponse answer = knowledgeAnswerService.answer(
                new KnowledgeSearchRequest(question, request.maxResults()),
                history
        );
        ConversationMessageResponse assistantMessage =
                persistenceService.saveAssistantMessage(
                        conversationId,
                        authenticatedUser.getId(),
                        answer
                );
        return new ConversationExchangeResponse(userMessage, assistantMessage);
    }

    @PreAuthorize("isAuthenticated()")
    public PagedResponse<ConversationMessageResponse> findHistory(
            UUID conversationId,
            User authenticatedUser,
            int page,
            int size
    ) {
        return persistenceService.findHistory(
                conversationId,
                authenticatedUser.getId(),
                page,
                size
        );
    }
}
