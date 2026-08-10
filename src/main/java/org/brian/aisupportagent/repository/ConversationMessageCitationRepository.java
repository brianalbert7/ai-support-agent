package org.brian.aisupportagent.repository;

import org.brian.aisupportagent.entity.ConversationMessageCitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMessageCitationRepository
        extends JpaRepository<ConversationMessageCitation, UUID> {

    List<ConversationMessageCitation> findAllByMessageIdInOrderByMessageIdAscSourceNumberAsc(
            Collection<UUID> messageIds
    );
}
