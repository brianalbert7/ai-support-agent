package org.brian.aisupportagent.service;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.ConversationResponse;
import org.brian.aisupportagent.dto.CreateConversationRequest;
import org.brian.aisupportagent.entity.Conversation;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.ConversationNotFoundException;
import org.brian.aisupportagent.repository.ConversationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ConversationResponse create(
            CreateConversationRequest request,
            User authenticatedUser
    ) {
        Conversation conversation = Conversation.builder()
                .owner(authenticatedUser)
                .title(request.title().trim())
                .build();
        return toResponse(conversationRepository.saveAndFlush(conversation));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<ConversationResponse> findAllFor(User authenticatedUser) {
        return conversationRepository
                .findAllByOwnerIdOrderByUpdatedAtDesc(authenticatedUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ConversationResponse findOwned(
            UUID conversationId,
            User authenticatedUser
    ) {
        return conversationRepository
                .findByIdAndOwnerId(conversationId, authenticatedUser.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
