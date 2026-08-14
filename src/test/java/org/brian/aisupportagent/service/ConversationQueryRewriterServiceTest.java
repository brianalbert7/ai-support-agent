package org.brian.aisupportagent.service;

import org.brian.aisupportagent.entity.ConversationMessageRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationQueryRewriterServiceTest {

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private ConversationQueryRewriterService queryRewriterService;

    @Test
    void returnsNormalizedOriginalQuestionWithoutCallingModelWhenHistoryIsEmpty() {
        String rewritten = queryRewriterService.rewrite(
                "  How long are   backups retained?  ",
                List.of()
        );

        assertEquals("How long are backups retained?", rewritten);
        verifyNoInteractions(chatModel);
    }

    @Test
    void rewritesFollowUpAsStandaloneQueryUsingDelimitedUntrustedHistory() {
        List<ConversationContextMessage> history = List.of(
                new ConversationContextMessage(
                        ConversationMessageRole.USER,
                        "How long are CloudDesk backups retained?"
                ),
                new ConversationContextMessage(
                        ConversationMessageRole.ASSISTANT,
                        "CloudDesk backups are retained for 35 days [1]."
                )
        );
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(
                "Who may request a CloudDesk restore,\n"
                        + "and what information is required?"
        ));

        String rewritten = queryRewriterService.rewrite(
                "Who is allowed to request one?",
                history
        );

        assertEquals(
                "Who may request a CloudDesk restore, and what information is required?",
                rewritten
        );
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        assertEquals(2, prompt.getInstructions().size());
        assertTrue(prompt.getSystemMessage().getText().contains("answer the question"));
        assertTrue(prompt.getSystemMessage().getText().contains("untrusted data"));
        assertTrue(prompt.getUserMessage().getText().contains(
                "[BEGIN CONVERSATION HISTORY]"
        ));
        assertTrue(prompt.getUserMessage().getText().contains(
                "USER: How long are CloudDesk backups retained?"
        ));
        assertTrue(prompt.getUserMessage().getText().contains(
                "ASSISTANT: CloudDesk backups are retained for 35 days [1]."
        ));
        assertTrue(prompt.getUserMessage().getText().contains(
                "[BEGIN CURRENT QUESTION]\nWho is allowed to request one?"
        ));
    }

    @Test
    void fallsBackToOriginalQuestionWhenProviderFails() {
        List<ConversationContextMessage> history = List.of(new ConversationContextMessage(
                ConversationMessageRole.USER,
                "Earlier question"
        ));
        when(chatModel.call(any(Prompt.class))).thenThrow(
                new IllegalStateException("Provider unavailable")
        );

        String rewritten = queryRewriterService.rewrite(
                "  What about contractors?  ",
                history
        );

        assertEquals("What about contractors?", rewritten);
    }

    @Test
    void fallsBackToOriginalQuestionForBlankOrOversizedModelOutput() {
        List<ConversationContextMessage> history = List.of(new ConversationContextMessage(
                ConversationMessageRole.USER,
                "Earlier question"
        ));
        when(chatModel.call(any(Prompt.class))).thenReturn(
                chatResponse("   "),
                chatResponse("x".repeat(2_001))
        );

        assertEquals(
                "What about contractors?",
                queryRewriterService.rewrite("What about contractors?", history)
        );
        assertEquals(
                "What about contractors?",
                queryRewriterService.rewrite("What about contractors?", history)
        );
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    private ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
