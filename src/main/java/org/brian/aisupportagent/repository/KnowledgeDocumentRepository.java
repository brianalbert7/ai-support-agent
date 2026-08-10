package org.brian.aisupportagent.repository;

import org.brian.aisupportagent.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    Page<KnowledgeDocument> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    boolean existsByChecksumSha256(String checksumSha256);
}
