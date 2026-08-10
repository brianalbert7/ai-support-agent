package org.brian.aisupportagent.service;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.dto.ConversationMessageResponse;
import org.brian.aisupportagent.dto.KnowledgeAnswerResponse;
import org.brian.aisupportagent.dto.KnowledgeCitationResponse;
import org.brian.aisupportagent.entity.Conversation;
import org.brian.aisupportagent.entity.ConversationMessage;
import org.brian.aisupportagent.entity.ConversationMessageCitation;
import org.brian.aisupportagent.entity.ConversationMessageRole;
import org.brian.aisupportagent.exception.ConversationNotFoundException;
import org.brian.aisupportagent.repository.ConversationMessageCitationRepository;
import org.brian.aisupportagent.repository.ConversationMessageRepository;
import org.brian.aisupportagent.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationMessagePersistenceService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationMessageCitationRepository citationRepository;

    @Transactional
    public ConversationMessageResponse saveUserMessage(
            UUID conversationId,
            UUID ownerId,
            String question
    ) {
        Conversation conversation = findOwnedConversation(conversationId, ownerId);
        ConversationMessage message = ConversationMessage.builder()
                .conversation(conversation)
                .role(ConversationMessageRole.USER)
                .content(question)
                .grounded(false)
                .build();
        ConversationMessage savedMessage = messageRepository.saveAndFlush(message);
        conversationRepository.touch(conversationId);
        return toResponse(savedMessage, List.of());
    }

    @Transactional
    public ConversationMessageResponse saveAssistantMessage(
            UUID conversationId,
            UUID ownerId,
            KnowledgeAnswerResponse answer
    ) {
        Conversation conversation = findOwnedConversation(conversationId, ownerId);
        ConversationMessage message = ConversationMessage.builder()
                .conversation(conversation)
                .role(ConversationMessageRole.ASSISTANT)
                .content(answer.answer())
                .grounded(answer.grounded())
                .build();
        ConversationMessage savedMessage = messageRepository.saveAndFlush(message);
        List<ConversationMessageCitation> savedCitations = citationRepository.saveAllAndFlush(
                answer.citations().stream()
                        .map(citation -> toEntity(citation, savedMessage))
                        .toList()
        );
        conversationRepository.touch(conversationId);
        return toResponse(savedMessage, savedCitations);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageResponse> findHistory(
            UUID conversationId,
            UUID ownerId
    ) {
        findOwnedConversation(conversationId, ownerId);
        List<ConversationMessage> messages = messageRepository
                .findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId);
        if (messages.isEmpty()) {
            return List.of();
        }

        List<UUID> messageIds = messages.stream()
                .map(ConversationMessage::getId)
                .toList();
        Map<UUID, List<ConversationMessageCitation>> citationsByMessage = citationRepository
                .findAllByMessageIdInOrderByMessageIdAscSourceNumberAsc(messageIds)
                .stream()
                .collect(Collectors.groupingBy(
                        citation -> citation.getMessage().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return messages.stream()
                .map(message -> toResponse(
                        message,
                        citationsByMessage.getOrDefault(message.getId(), List.of())
                ))
                .toList();
    }

    private Conversation findOwnedConversation(UUID conversationId, UUID ownerId) {
        return conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    private ConversationMessageCitation toEntity(
            KnowledgeCitationResponse citation,
            ConversationMessage message
    ) {
        return ConversationMessageCitation.builder()
                .message(message)
                .sourceNumber(citation.sourceNumber())
                .chunkId(citation.chunkId())
                .documentId(citation.documentId())
                .documentName(citation.documentName())
                .pageNumber(citation.pageNumber())
                .excerpt(citation.excerpt())
                .similarity(citation.similarity())
                .build();
    }

    private ConversationMessageResponse toResponse(
            ConversationMessage message,
            List<ConversationMessageCitation> citations
    ) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.isGrounded(),
                citations.stream().map(this::toResponse).toList(),
                message.getCreatedAt()
        );
    }

    private KnowledgeCitationResponse toResponse(ConversationMessageCitation citation) {
        return new KnowledgeCitationResponse(
                citation.getSourceNumber(),
                citation.getChunkId(),
                citation.getDocumentId(),
                citation.getDocumentName(),
                citation.getPageNumber(),
                citation.getExcerpt(),
                citation.getSimilarity()
        );
    }
}
