package org.brian.aisupportagent.repository;

import org.brian.aisupportagent.entity.KnowledgeDocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeDocumentChunkRepository
        extends JpaRepository<KnowledgeDocumentChunk, UUID> {

    @Query("""
            select chunk
            from KnowledgeDocumentChunk chunk
            join fetch chunk.knowledgeDocumentPage page
            where page.knowledgeDocument.id = :documentId
            order by page.pageNumber, chunk.chunkIndex
            """)
    List<KnowledgeDocumentChunk> findAllByDocumentIdOrdered(
            @Param("documentId") UUID documentId
    );
}
