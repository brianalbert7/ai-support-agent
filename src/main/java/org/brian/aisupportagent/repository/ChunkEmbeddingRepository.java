package org.brian.aisupportagent.repository;

import lombok.RequiredArgsConstructor;
import org.brian.aisupportagent.service.ChunkEmbedding;
import org.brian.aisupportagent.service.RetrievedDocumentChunk;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChunkEmbeddingRepository {

    private static final String UPDATE_EMBEDDING_SQL = """
            UPDATE knowledge_document_chunks
            SET embedding = CAST(? AS vector)
            WHERE id = ?
            """;

    private static final String FIND_SIMILAR_SQL = """
            SELECT chunk.id AS chunk_id,
                   document.id AS document_id,
                   document.display_name AS document_name,
                   page.page_number,
                   chunk.chunk_index,
                   chunk.content,
                   1 - (chunk.embedding <=> CAST(:embedding AS vector)) AS similarity
            FROM knowledge_document_chunks chunk
            JOIN knowledge_document_pages page
              ON page.id = chunk.knowledge_document_page_id
            JOIN knowledge_documents document
              ON document.id = page.knowledge_document_id
            WHERE document.status = 'READY'
              AND chunk.embedding IS NOT NULL
              AND 1 - (chunk.embedding <=> CAST(:embedding AS vector))
                  >= :minimumSimilarity
            ORDER BY chunk.embedding <=> CAST(:embedding AS vector)
            LIMIT :resultLimit
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void saveAll(List<ChunkEmbedding> embeddings) {
        int[] updateCounts = jdbcTemplate.batchUpdate(
                UPDATE_EMBEDDING_SQL,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(
                            @NonNull PreparedStatement statement,
                            int index
                    ) throws SQLException {
                        ChunkEmbedding embedding = embeddings.get(index);
                        statement.setString(1, toVectorLiteral(embedding.vector()));
                        statement.setObject(2, embedding.chunk().getId());
                    }

                    @Override
                    public int getBatchSize() {
                        return embeddings.size();
                    }
                }
        );

        for (int updateCount : updateCounts) {
            if (updateCount == 0) {
                throw new IllegalStateException("A document chunk embedding was not stored");
            }
        }
    }

    public int countEmbeddedByDocumentId(UUID documentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM knowledge_document_chunks chunk
                        JOIN knowledge_document_pages page
                          ON page.id = chunk.knowledge_document_page_id
                        WHERE page.knowledge_document_id = ?
                          AND chunk.embedding IS NOT NULL
                        """,
                Integer.class,
                documentId
        );
        return count == null ? 0 : count;
    }

    public List<RetrievedDocumentChunk> findSimilar(
            float[] queryEmbedding,
            double minimumSimilarity,
            int resultLimit
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("embedding", toVectorLiteral(queryEmbedding))
                .addValue("minimumSimilarity", minimumSimilarity)
                .addValue("resultLimit", resultLimit);

        return namedParameterJdbcTemplate.query(
                FIND_SIMILAR_SQL,
                parameters,
                (resultSet, rowNumber) -> new RetrievedDocumentChunk(
                        resultSet.getObject("chunk_id", UUID.class),
                        resultSet.getObject("document_id", UUID.class),
                        resultSet.getString("document_name"),
                        resultSet.getInt("page_number"),
                        resultSet.getInt("chunk_index"),
                        resultSet.getString("content"),
                        resultSet.getDouble("similarity")
                )
        );
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder literal = new StringBuilder(vector.length * 12).append('[');
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                literal.append(',');
            }
            literal.append(Float.toString(vector[index]));
        }
        return literal.append(']').toString();
    }
}
