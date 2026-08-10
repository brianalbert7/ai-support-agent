package org.brian.aisupportagent.service;

import org.brian.aisupportagent.config.ConversationHistoryProperties;
import org.brian.aisupportagent.dto.ConversationMessageResponse;
import org.brian.aisupportagent.dto.PagedResponse;
import org.brian.aisupportagent.entity.Conversation;
import org.brian.aisupportagent.entity.ConversationMessage;
import org.brian.aisupportagent.entity.ConversationMessageCitation;
import org.brian.aisupportagent.entity.ConversationMessageRole;
import org.brian.aisupportagent.repository.ConversationMessageCitationRepository;
import org.brian.aisupportagent.repository.ConversationMessageRepository;
import org.brian.aisupportagent.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;

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

    @Test
    void loadsOnlyRequestedMessagePageAndItsCitations() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ConversationMessage userMessage = message(
                ConversationMessageRole.USER,
                "Question"
        );
        ConversationMessage assistantMessage = message(
                ConversationMessageRole.ASSISTANT,
                "Grounded answer [1]."
        );
        assistantMessage.setGrounded(true);
        ConversationMessageCitation citation = ConversationMessageCitation.builder()
                .message(assistantMessage)
                .sourceNumber(1)
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .documentName("Employee Handbook")
                .pageNumber(4)
                .excerpt("Source excerpt")
                .similarity(0.91)
                .build();
        PageRequest pageRequest = PageRequest.of(1, 2);
        when(conversationRepository.findByIdAndOwnerId(conversationId, ownerId))
                .thenReturn(Optional.of(new Conversation()));
        when(messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(
                conversationId,
                pageRequest
        )).thenReturn(new PageImpl<>(
                List.of(userMessage, assistantMessage),
                pageRequest,
                5
        ));
        List<UUID> pageMessageIds = List.of(
                userMessage.getId(),
                assistantMessage.getId()
        );
        when(citationRepository
                .findAllByMessageIdInOrderByMessageIdAscSourceNumberAsc(pageMessageIds))
                .thenReturn(List.of(citation));
        ConversationMessagePersistenceService service = service();

        PagedResponse<ConversationMessageResponse> response = service.findHistory(
                conversationId,
                ownerId,
                1,
                2
        );

        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(2, response.content().size());
        assertEquals(List.of(), response.content().getFirst().citations());
        assertEquals(1, response.content().getLast().citations().size());
        assertEquals(
                "Employee Handbook",
                response.content().getLast().citations().getFirst().documentName()
        );
        verify(citationRepository)
                .findAllByMessageIdInOrderByMessageIdAscSourceNumberAsc(pageMessageIds);
    }

    private ConversationMessagePersistenceService service() {
        return new ConversationMessagePersistenceService(
                conversationRepository,
                messageRepository,
                citationRepository,
                new ConversationHistoryProperties(10, 12000)
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
