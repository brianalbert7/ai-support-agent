package org.brian.aisupportagent.service;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.ConversationResponse;
import org.brian.aisupportagent.dto.CreateConversationRequest;
import org.brian.aisupportagent.dto.PagedResponse;
import org.brian.aisupportagent.dto.UpdateConversationRequest;
import org.brian.aisupportagent.entity.Conversation;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.exception.ConversationNotFoundException;
import org.brian.aisupportagent.repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public PagedResponse<ConversationResponse> findAllFor(
            User authenticatedUser,
            int page,
            int size
    ) {
        Page<Conversation> conversationPage = conversationRepository
                .findAllByOwnerIdOrderByUpdatedAtDescIdDesc(
                        authenticatedUser.getId(),
                        PageRequest.of(page, size)
                );
        return new PagedResponse<>(
                conversationPage.getContent().stream()
                        .map(this::toResponse)
                        .toList(),
                conversationPage.getNumber(),
                conversationPage.getSize(),
                conversationPage.getTotalElements(),
                conversationPage.getTotalPages(),
                conversationPage.isFirst(),
                conversationPage.isLast()
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ConversationResponse findOwned(
            UUID conversationId,
            User authenticatedUser
    ) {
        return toResponse(findOwnedEntity(conversationId, authenticatedUser.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ConversationResponse update(
            UUID conversationId,
            UpdateConversationRequest request,
            User authenticatedUser
    ) {
        Conversation conversation = findOwnedEntity(
                conversationId,
                authenticatedUser.getId()
        );
        conversation.setTitle(request.title().trim());
        conversationRepository.flush();
        return toResponse(conversation);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void delete(UUID conversationId, User authenticatedUser) {
        Conversation conversation = findOwnedEntity(
                conversationId,
                authenticatedUser.getId()
        );
        conversationRepository.delete(conversation);
        conversationRepository.flush();
    }

    private Conversation findOwnedEntity(UUID conversationId, UUID ownerId) {
        return conversationRepository
                .findByIdAndOwnerId(conversationId, ownerId)
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
