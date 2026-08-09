package org.brian.aisupportagent.integration;

import com.jayway.jsonpath.JsonPath;
import org.brian.aisupportagent.entity.Role;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "JWT_SECRET=VGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXRoYXQtaXMtMzItYnl0ZXMh",
        "spring.ai.openai.api-key=test-api-key"
})
@AutoConfigureMockMvc
class AuthenticationHttpIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationPersistsUserAndAccessTokenAuthenticatesCurrentUser() throws Exception {
        String email = uniqueEmail();

        AuthenticationTokens tokens = register(email);

        User savedUser = userRepository.findByEmail(email).orElseThrow();
        assertEquals(Role.EMPLOYEE, savedUser.getRole());
        assertNotEquals(PASSWORD, savedUser.getPassword());
        assertTrue(passwordEncoder.matches(PASSWORD, savedUser.getPassword()));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void loginNormalizesEmailAndRejectsIncorrectPassword() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email.toUpperCase(),
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "IncorrectPassword123!"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refreshRotatesTokenAndLogoutRevokesReplacement() throws Exception {
        AuthenticationTokens originalTokens = register(uniqueEmail());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest(originalTokens.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthenticationTokens rotatedTokens = readTokens(refreshResult);
        assertNotEquals(originalTokens.refreshToken(), rotatedTokens.refreshToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest(originalTokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest(rotatedTokens.refreshToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest(rotatedTokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void protectedEndpointRejectsMissingAndMalformedAccessTokens() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private AuthenticationTokens register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "firstName", "Ada",
                                "lastName", "Lovelace",
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        return readTokens(result);
    }

    private AuthenticationTokens readTokens(MvcResult result) {
        String responseBody = new String(
                result.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8
        );
        return new AuthenticationTokens(
                JsonPath.read(responseBody, "$.accessToken"),
                JsonPath.read(responseBody, "$.refreshToken")
        );
    }

    private String refreshRequest(String refreshToken) {
        return json(Map.of("refreshToken", refreshToken));
    }

    private String json(Map<String, String> body) {
        return objectMapper.writeValueAsString(body);
    }

    private String uniqueEmail() {
        return "ada-" + UUID.randomUUID() + "@example.com";
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private record AuthenticationTokens(String accessToken, String refreshToken) {
    }
}
