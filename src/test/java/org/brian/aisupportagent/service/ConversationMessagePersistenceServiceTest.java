package org.brian.aisupportagent.service;

import org.brian.aisupportagent.config.ConversationHistoryProperties;
import org.brian.aisupportagent.entity.Conversation;
import org.brian.aisupportagent.entity.ConversationMessage;
import org.brian.aisupportagent.entity.ConversationMessageRole;
import org.brian.aisupportagent.repository.ConversationMessageCitationRepository;
import org.brian.aisupportagent.repository.ConversationMessageRepository;
import org.brian.aisupportagent.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMessagePersistenceServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMessageRepository messageRepository;

    @Mock
    private ConversationMessageCitationRepository citationRepository;

    @Test
    void limitsContextByCountAndCharactersThenRestoresChronologicalOrder() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID currentMessageId = UUID.randomUUID();
        ConversationMessage newestAssistant = message(
                ConversationMessageRole.ASSISTANT,
                "a".repeat(600)
        );
        ConversationMessage olderUser = message(
                ConversationMessageRole.USER,
                "u".repeat(500)
        );
        when(conversationRepository.findByIdAndOwnerId(conversationId, ownerId))
                .thenReturn(Optional.of(new Conversation()));
        when(messageRepository
                .findAllByConversationIdAndIdNotOrderByCreatedAtDescIdDesc(
                        conversationId,
                        currentMessageId,
                        PageRequest.of(0, 2)
                ))
                .thenReturn(List.of(newestAssistant, olderUser));
        ConversationMessagePersistenceService service =
                new ConversationMessagePersistenceService(
                        conversationRepository,
                        messageRepository,
                        citationRepository,
                        new ConversationHistoryProperties(2, 1000)
                );

        List<ConversationContextMessage> context = service.findRecentContext(
                conversationId,
                ownerId,
                currentMessageId
        );

        assertEquals(1, context.size());
        assertEquals(ConversationMessageRole.ASSISTANT, context.getFirst().role());
        assertEquals(600, context.getFirst().content().length());
        verify(messageRepository)
                .findAllByConversationIdAndIdNotOrderByCreatedAtDescIdDesc(
                        conversationId,
                        currentMessageId,
                        PageRequest.of(0, 2)
                );
    }

    private ConversationMessage message(
            ConversationMessageRole role,
            String content
    ) {
        return ConversationMessage.builder()
                .id(UUID.randomUUID())
                .role(role)
                .content(content)
                .build();
    }
}
