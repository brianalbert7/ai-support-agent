package org.brian.aisupportagent.repository;

import org.brian.aisupportagent.entity.KnowledgeDocumentPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeDocumentPageRepository
        extends JpaRepository<KnowledgeDocumentPage, UUID> {

    List<KnowledgeDocumentPage> findAllByKnowledgeDocumentIdOrderByPageNumberAsc(
            UUID knowledgeDocumentId
    );

    @Modifying(flushAutomatically = true)
    @Query("delete from KnowledgeDocumentPage page "
            + "where page.knowledgeDocument.id = :documentId")
    void deleteAllByKnowledgeDocumentId(@Param("documentId") UUID documentId);
}
