package org.brian.aisupportagent.service;

import org.brian.aisupportagent.dto.ConversationResponse;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
