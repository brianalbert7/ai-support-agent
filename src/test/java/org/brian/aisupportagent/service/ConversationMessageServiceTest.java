package org.brian.aisupportagent.service;

import org.brian.aisupportagent.dto.AskConversationRequest;
import org.brian.aisupportagent.dto.ConversationExchangeResponse;
import org.brian.aisupportagent.dto.ConversationMessageResponse;
import org.brian.aisupportagent.dto.KnowledgeAnswerResponse;
import org.brian.aisupportagent.dto.KnowledgeSearchRequest;
import org.brian.aisupportagent.entity.ConversationMessageRole;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.KnowledgeAnswerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMessageServiceTest {

    @Mock
    private ConversationMessagePersistenceService persistenceService;

    @Mock
    private KnowledgeAnswerService knowledgeAnswerService;

    @InjectMocks
    private ConversationMessageService conversationMessageService;

    @Test
    void storesNormalizedQuestionThenGeneratedAnswer() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();
        AskConversationRequest request = new AskConversationRequest(
                "  How many vacation days do I get?  ",
                4
        );
        ConversationMessageResponse userMessage = message(
                ConversationMessageRole.USER,
                "How many vacation days do I get?"
        );
        KnowledgeAnswerResponse answer = new KnowledgeAnswerResponse(
                "How many vacation days do I get?",
                "Employees receive twenty vacation days [1].",
                true,
                List.of()
        );
        ConversationMessageResponse assistantMessage = message(
                ConversationMessageRole.ASSISTANT,
                answer.answer()
        );
        List<ConversationContextMessage> history = List.of(
                new ConversationContextMessage(
                        ConversationMessageRole.USER,
                        "Earlier question"
                ),
                new ConversationContextMessage(
                        ConversationMessageRole.ASSISTANT,
                        "Earlier answer [1]."
                )
        );
        when(persistenceService.saveUserMessage(
                conversationId,
                ownerId,
                "How many vacation days do I get?"
        )).thenReturn(userMessage);
        when(persistenceService.findRecentContext(
                conversationId,
                ownerId,
                userMessage.id()
        )).thenReturn(history);
        when(knowledgeAnswerService.answer(
                new KnowledgeSearchRequest("How many vacation days do I get?", 4),
                history
        )).thenReturn(answer);
        when(persistenceService.saveAssistantMessage(
                conversationId,
                ownerId,
                answer
        )).thenReturn(assistantMessage);

        ConversationExchangeResponse response = conversationMessageService.ask(
                conversationId,
                request,
                user
        );

        assertEquals(userMessage, response.userMessage());
        assertEquals(assistantMessage, response.assistantMessage());
        verify(persistenceService).saveUserMessage(
                conversationId,
                ownerId,
                "How many vacation days do I get?"
        );
        verify(persistenceService).findRecentContext(
                conversationId,
                ownerId,
                userMessage.id()
        );
        verify(knowledgeAnswerService).answer(
                new KnowledgeSearchRequest("How many vacation days do I get?", 4),
                history
        );
        verify(persistenceService).saveAssistantMessage(conversationId, ownerId, answer);
    }

    @Test
    void leavesPersistedQuestionUnansweredWhenAnswerGenerationFails() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();
        AskConversationRequest request = new AskConversationRequest("Reset password", null);
        KnowledgeSearchRequest searchRequest = new KnowledgeSearchRequest(
                "Reset password",
                null
        );
        ConversationMessageResponse userMessage = message(
                ConversationMessageRole.USER,
                "Reset password"
        );
        when(persistenceService.saveUserMessage(
                conversationId,
                ownerId,
                "Reset password"
        )).thenReturn(userMessage);
        when(persistenceService.findRecentContext(
                conversationId,
                ownerId,
                userMessage.id()
        )).thenReturn(List.of());
        when(knowledgeAnswerService.answer(searchRequest, List.of())).thenThrow(
                new KnowledgeAnswerException(new IllegalStateException("Provider unavailable"))
        );

        assertThrows(
                KnowledgeAnswerException.class,
                () -> conversationMessageService.ask(conversationId, request, user)
        );

        verify(persistenceService, never()).saveAssistantMessage(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void returnsHistoryForAuthenticatedOwner() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();
        List<ConversationMessageResponse> history = List.of(
                message(ConversationMessageRole.USER, "Question"),
                message(ConversationMessageRole.ASSISTANT, "Answer")
        );
        when(persistenceService.findHistory(conversationId, ownerId)).thenReturn(history);

        assertEquals(
                history,
                conversationMessageService.findHistory(conversationId, user)
        );
    }

    private ConversationMessageResponse message(
            ConversationMessageRole role,
            String content
    ) {
        return new ConversationMessageResponse(
                UUID.randomUUID(),
                role,
                content,
                role == ConversationMessageRole.ASSISTANT,
                List.of(),
                Instant.now()
        );
    }
}
