package org.brian.aisupportagent.repository;

import org.brian.aisupportagent.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    Optional<Conversation> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    UPDATE conversations
                    SET updated_at = CURRENT_TIMESTAMP
                    WHERE id = :conversationId
                    """,
            nativeQuery = true
    )
    void touch(UUID conversationId);
}
