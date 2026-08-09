package org.brian.aisupportagent.integration;

import org.brian.aisupportagent.entity.RefreshToken;
import org.brian.aisupportagent.entity.Role;
import org.brian.aisupportagent.entity.User;
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
    private JdbcClient jdbcClient;

    @Test
    @Transactional
    void flywayMigrationMatchesJpaMappings() {
        Integer migrationCount = jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE version = '1'
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

        assertEquals(1, migrationCount);
        assertTrue(userRepository.findByEmail("employee@example.com").isPresent());
        assertTrue(refreshTokenRepository.findByTokenHash("a".repeat(64)).isPresent());
    }
}
