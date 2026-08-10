package org.brian.aisupportagent.service;

import org.brian.aisupportagent.dto.ConversationResponse;
import org.brian.aisupportagent.dto.PagedResponse;
import org.brian.aisupportagent.dto.UpdateConversationRequest;
import org.brian.aisupportagent.entity.Conversation;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.ConversationNotFoundException;
import org.brian.aisupportagent.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void returnsRequestedPageOfOwnedConversations() {
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).build();
        Conversation firstConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .title("Most recently updated")
                .createdAt(Instant.parse("2026-08-09T12:00:00Z"))
                .updatedAt(Instant.parse("2026-08-09T14:00:00Z"))
                .build();
        Conversation secondConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .title("Next conversation")
                .createdAt(Instant.parse("2026-08-09T11:00:00Z"))
                .updatedAt(Instant.parse("2026-08-09T13:00:00Z"))
                .build();
        PageRequest pageRequest = PageRequest.of(1, 2);
        when(conversationRepository.findAllByOwnerIdOrderByUpdatedAtDescIdDesc(
                ownerId,
                pageRequest
        )).thenReturn(new PageImpl<>(
                List.of(firstConversation, secondConversation),
                pageRequest,
                5
        ));

        PagedResponse<ConversationResponse> response = conversationService.findAllFor(
                owner,
                1,
                2
        );

        assertEquals(List.of(
                "Most recently updated",
                "Next conversation"
        ), response.content().stream().map(ConversationResponse::title).toList());
        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertFalse(response.first());
        assertFalse(response.last());
        verify(conversationRepository).findAllByOwnerIdOrderByUpdatedAtDescIdDesc(
                ownerId,
                pageRequest
        );
    }

    @Test
    void updatesOwnedConversationWithNormalizedTitle() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).build();
        Instant createdAt = Instant.parse("2026-08-09T12:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-09T13:00:00Z");
        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .owner(owner)
                .title("Original title")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
        when(conversationRepository.findByIdAndOwnerId(conversationId, ownerId))
                .thenReturn(Optional.of(conversation));

        ConversationResponse response = conversationService.update(
                conversationId,
                new UpdateConversationRequest("  Updated title  "),
                owner
        );

        assertEquals("Updated title", conversation.getTitle());
        assertEquals(conversationId, response.id());
        assertEquals("Updated title", response.title());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        verify(conversationRepository).flush();
    }

    @Test
    void deletesOwnedConversationAndFlushesDatabaseCascade() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).build();
        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .owner(owner)
                .title("Conversation to delete")
                .build();
        when(conversationRepository.findByIdAndOwnerId(conversationId, ownerId))
                .thenReturn(Optional.of(conversation));

        conversationService.delete(conversationId, owner);

        verify(conversationRepository).delete(conversation);
        verify(conversationRepository).flush();
    }

    @Test
    void rejectsDeleteWhenConversationIsNotOwnedByAuthenticatedUser() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        User user = User.builder().id(ownerId).build();
        when(conversationRepository.findByIdAndOwnerId(conversationId, ownerId))
                .thenReturn(Optional.empty());

        assertThrows(
                ConversationNotFoundException.class,
                () -> conversationService.delete(conversationId, user)
        );

        verify(conversationRepository, never()).delete(
                org.mockito.ArgumentMatchers.any(Conversation.class)
        );
        verify(conversationRepository, never()).flush();
    }
}
