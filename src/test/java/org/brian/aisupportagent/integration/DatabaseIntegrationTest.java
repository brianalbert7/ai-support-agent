package org.brian.aisupportagent.integration;

import org.brian.aisupportagent.entity.DocumentStatus;
import org.brian.aisupportagent.entity.KnowledgeDocument;
import org.brian.aisupportagent.entity.RefreshToken;
import org.brian.aisupportagent.entity.Role;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.repository.KnowledgeDocumentRepository;
import org.brian.aisupportagent.repository.RefreshTokenRepository;
import org.brian.aisupportagent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "JWT_SECRET=VGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXRoYXQtaXMtMzItYnl0ZXMh",
        "spring.ai.openai.api-key=test-api-key"
})
class DatabaseIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine")
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    @Transactional
    void flywayMigrationMatchesJpaMappings() {
        Integer migrationCount = jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE version IN ('1', '2')
                          AND success = TRUE
                        """)
                .query(Integer.class)
                .single();

        User user = User.builder()
                .firstName("Test")
                .lastName("Employee")
                .email("employee@example.com")
                .password("encoded-password")
                .role(Role.EMPLOYEE)
                .build();
        User savedUser = userRepository.saveAndFlush(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash("a".repeat(64))
                .expiration(Instant.now().plusSeconds(3600))
                .user(savedUser)
                .build();
        refreshTokenRepository.saveAndFlush(refreshToken);

        KnowledgeDocument document = KnowledgeDocument.builder()
                .displayName("Employee Handbook")
                .originalFileName("employee-handbook.pdf")
                .contentType("application/pdf")
                .sizeBytes(2048)
                .storageKey("documents/employee-handbook.pdf")
                .checksumSha256("b".repeat(64))
                .status(DocumentStatus.UPLOADED)
                .uploadedBy(savedUser)
                .build();
        KnowledgeDocument savedDocument = knowledgeDocumentRepository.saveAndFlush(document);

        assertEquals(2, migrationCount);
        assertTrue(userRepository.findByEmail("employee@example.com").isPresent());
        assertTrue(refreshTokenRepository.findByTokenHash("a".repeat(64)).isPresent());
        assertEquals(DocumentStatus.UPLOADED, savedDocument.getStatus());
        assertEquals(savedUser.getId(), savedDocument.getUploadedBy().getId());
        assertNotNull(savedDocument.getCreatedAt());
        assertNotNull(savedDocument.getUpdatedAt());
    }
}
