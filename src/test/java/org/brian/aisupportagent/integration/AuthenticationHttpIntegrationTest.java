package org.brian.aisupportagent.integration;

import com.jayway.jsonpath.JsonPath;
import org.brian.aisupportagent.entity.DocumentStatus;
import org.brian.aisupportagent.entity.KnowledgeDocument;
import org.brian.aisupportagent.entity.KnowledgeDocumentChunk;
import org.brian.aisupportagent.entity.KnowledgeDocumentPage;
import org.brian.aisupportagent.entity.Role;
import org.brian.aisupportagent.entity.User;
import org.brian.aisupportagent.repository.KnowledgeDocumentChunkRepository;
import org.brian.aisupportagent.repository.ChunkEmbeddingRepository;
import org.brian.aisupportagent.repository.KnowledgeDocumentRepository;
import org.brian.aisupportagent.repository.KnowledgeDocumentPageRepository;
import org.brian.aisupportagent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.brian.aisupportagent.util.PdfTestData.pdfWithPages;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "JWT_SECRET=VGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXRoYXQtaXMtMzItYnl0ZXMh",
        "spring.ai.openai.api-key=test-api-key"
})
@AutoConfigureMockMvc
class AuthenticationHttpIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";
    private static final Path DOCUMENT_STORAGE_DIRECTORY = Path.of(
            System.getProperty("java.io.tmpdir"),
            "ai-support-agent-tests-" + UUID.randomUUID()
    );

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:0.8.2-pg16")
                    .asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void documentStorageProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "app.storage.documents.directory",
                () -> DOCUMENT_STORAGE_DIRECTORY.toString()
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KnowledgeDocumentRepository documentRepository;

    @Autowired
    private KnowledgeDocumentPageRepository documentPageRepository;

    @Autowired
    private KnowledgeDocumentChunkRepository documentChunkRepository;

    @Autowired
    private ChunkEmbeddingRepository chunkEmbeddingRepository;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @MockitoBean
    private ChatModel chatModel;

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

    @Test
    void adminEndpointForbidsEmployeesAndAllowsAdmins() throws Exception {
        String employeeEmail = uniqueEmail();
        AuthenticationTokens employeeTokens = register(employeeEmail);

        String adminEmail = uniqueEmail();
        register(adminEmail);
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);
        AuthenticationTokens adminTokens = login(adminEmail, PASSWORD);

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employeeTokens.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to access this resource"));

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItems(employeeEmail, adminEmail)))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void documentUploadForbidsEmployeesAndStoresAdminPdf() throws Exception {
        long documentCountBefore = documentRepository.count();
        long storedPdfCountBefore = storedPdfCount();
        AuthenticationTokens employeeTokens = register(uniqueEmail());
        AuthenticationTokens adminTokens = registerAdmin(uniqueEmail());
        byte[] pdfContent = ("%PDF-1.4\nportfolio-test-" + UUID.randomUUID())
                .getBytes(StandardCharsets.US_ASCII);

        mockMvc.perform(multipart("/api/admin/documents")
                        .file(pdfFile("employee-handbook.pdf", pdfContent))
                        .param("displayName", "Employee Handbook")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employeeTokens.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/documents")
                        .file(pdfFile("employee-handbook.pdf", pdfContent))
                        .param("displayName", "Employee Handbook")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Employee Handbook"))
                .andExpect(jsonPath("$.originalFileName").value("employee-handbook.pdf"))
                .andExpect(jsonPath("$.contentType").value(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(jsonPath("$.sizeBytes").value(pdfContent.length))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andReturn();

        String responseBody = new String(
                uploadResult.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8
        );
        UUID documentId = UUID.fromString(JsonPath.read(responseBody, "$.id"));
        KnowledgeDocument storedDocument = documentRepository.findById(documentId).orElseThrow();

        assertEquals(DocumentStatus.UPLOADED, storedDocument.getStatus());
        assertEquals(sha256(pdfContent), storedDocument.getChecksumSha256());
        assertTrue(Files.exists(
                DOCUMENT_STORAGE_DIRECTORY.resolve(storedDocument.getStorageKey())
        ));

        mockMvc.perform(multipart("/api/admin/documents")
                        .file(pdfFile("same-content-different-name.pdf", pdfContent))
                        .param("displayName", "Duplicate Handbook")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_ALREADY_EXISTS"));

        assertEquals(documentCountBefore + 1, documentRepository.count());
        assertEquals(storedPdfCountBefore + 1, storedPdfCount());
    }

    @Test
    void adminProcessesPdfIntoPageRecordsAndCannotProcessReadyDocumentAgain()
            throws Exception {
        when(embeddingModel.embed(anyList())).thenAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream()
                    .map(input -> testEmbeddingVector())
                    .toList();
        });
        AuthenticationTokens employeeTokens = register(uniqueEmail());
        AuthenticationTokens adminTokens = registerAdmin(uniqueEmail());
        String uniqueText = "vacation policy " + UUID.randomUUID();
        byte[] pdfContent = pdfWithPages(
                uniqueText,
                "",
                "reset a customer password from the account settings page"
        );
        UUID documentId = uploadDocument(adminTokens, "Support Handbook", pdfContent);

        mockMvc.perform(post("/api/admin/documents/{documentId}/process", documentId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(employeeTokens.accessToken())
                        ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/admin/documents/{documentId}/process", documentId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(adminTokens.accessToken())
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.pageCount").value(3));

        KnowledgeDocument processedDocument = documentRepository.findById(documentId)
                .orElseThrow();
        List<KnowledgeDocumentPage> pages = documentPageRepository
                .findAllByKnowledgeDocumentIdOrderByPageNumberAsc(documentId);
        List<KnowledgeDocumentChunk> chunks = documentChunkRepository
                .findAllByDocumentIdOrdered(documentId);

        assertEquals(DocumentStatus.READY, processedDocument.getStatus());
        assertEquals(3, processedDocument.getPageCount());
        assertEquals(2, pages.size());
        assertEquals(1, pages.getFirst().getPageNumber());
        assertEquals(uniqueText, pages.getFirst().getContent());
        assertEquals(3, pages.getLast().getPageNumber());
        assertEquals(2, chunks.size());
        assertEquals(0, chunks.getFirst().getChunkIndex());
        assertEquals(1, chunks.getFirst().getKnowledgeDocumentPage().getPageNumber());
        assertEquals(uniqueText, chunks.getFirst().getContent());
        assertEquals(0, chunks.getLast().getChunkIndex());
        assertEquals(3, chunks.getLast().getKnowledgeDocumentPage().getPageNumber());
        assertEquals(2, chunkEmbeddingRepository.countEmbeddedByDocumentId(documentId));

        mockMvc.perform(post("/api/admin/documents/{documentId}/process", documentId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(adminTokens.accessToken())
                        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_DOCUMENT_STATE"));
    }

    @Test
    void processingInvalidPdfCommitsFailedStatus() throws Exception {
        AuthenticationTokens adminTokens = registerAdmin(uniqueEmail());
        byte[] invalidPdf = ("%PDF-1.4\nbroken-" + UUID.randomUUID())
                .getBytes(StandardCharsets.US_ASCII);
        UUID documentId = uploadDocument(adminTokens, "Broken PDF", invalidPdf);

        mockMvc.perform(post("/api/admin/documents/{documentId}/process", documentId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(adminTokens.accessToken())
                        ))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DOCUMENT_PROCESSING_FAILED"))
                .andExpect(jsonPath("$.message").value("The PDF could not be processed"));

        KnowledgeDocument failedDocument = documentRepository.findById(documentId)
                .orElseThrow();
        assertEquals(DocumentStatus.FAILED, failedDocument.getStatus());
        assertNotNull(failedDocument.getFailureReason());
        assertEquals(0, documentPageRepository
                .findAllByKnowledgeDocumentIdOrderByPageNumberAsc(documentId)
                .size());
        assertEquals(0, documentChunkRepository.findAllByDocumentIdOrdered(documentId).size());
    }

    @Test
    void embeddingFailureCommitsFailedStatusWithoutPartialSearchData() throws Exception {
        when(embeddingModel.embed(anyList())).thenThrow(
                new IllegalStateException("Simulated embedding provider failure")
        );
        AuthenticationTokens adminTokens = registerAdmin(uniqueEmail());
        byte[] pdfContent = pdfWithPages(
                "This page contains enough searchable support policy text."
        );
        UUID documentId = uploadDocument(adminTokens, "Embedding Failure", pdfContent);

        mockMvc.perform(post("/api/admin/documents/{documentId}/process", documentId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(adminTokens.accessToken())
                        ))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DOCUMENT_PROCESSING_FAILED"));

        KnowledgeDocument failedDocument = documentRepository.findById(documentId)
                .orElseThrow();
        assertEquals(DocumentStatus.FAILED, failedDocument.getStatus());
        assertEquals(0, documentPageRepository
                .findAllByKnowledgeDocumentIdOrderByPageNumberAsc(documentId)
                .size());
        assertEquals(0, documentChunkRepository.findAllByDocumentIdOrdered(documentId).size());
        assertEquals(0, chunkEmbeddingRepository.countEmbeddedByDocumentId(documentId));
    }

    @Test
    void processingUnknownDocumentReturnsNotFound() throws Exception {
        AuthenticationTokens adminTokens = registerAdmin(uniqueEmail());

        mockMvc.perform(post("/api/admin/documents/{documentId}/process", UUID.randomUUID())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(adminTokens.accessToken())
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void documentUploadRejectsFakePdfContent() throws Exception {
        AuthenticationTokens adminTokens = registerAdmin(uniqueEmail());

        mockMvc.perform(multipart("/api/admin/documents")
                        .file(pdfFile(
                                "valid.pdf",
                                "%PDF-1.4\nvalid".getBytes(StandardCharsets.US_ASCII)
                        ))
                        .param("displayName", " ")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.displayName")
                        .value("Display name is required"));

        mockMvc.perform(multipart("/api/admin/documents")
                        .file(pdfFile(
                                "not-really-a-pdf.pdf",
                                "plain text".getBytes(StandardCharsets.UTF_8)
                        ))
                        .param("displayName", "Fake PDF")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DOCUMENT"))
                .andExpect(jsonPath("$.message").value("The uploaded file is not a valid PDF"));
    }

    @Test
    void authenticatedEmployeeSearchesReadyDocumentsBySemanticSimilarity()
            throws Exception {
        String bestText = "Vacation allowance policy " + UUID.randomUUID();
        String secondText = "Paid time off guidance " + UUID.randomUUID();
        String question = "How much vacation time do employees receive?";
        when(embeddingModel.embed(anyList())).thenAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream()
                    .map(input -> input.contains(bestText)
                            ? testEmbeddingVector(2, 1.0f, 3, 0.0f)
                            : testEmbeddingVector(2, 0.8f, 3, 0.6f))
                    .toList();
        });
        when(embeddingModel.embed(eq(question)))
                .thenReturn(testEmbeddingVector(2, 1.0f, 3, 0.0f));
        AuthenticationTokens employeeTokens = register(uniqueEmail());
        AuthenticationTokens adminTokens = registerAdmin(uniqueEmail());
        UUID bestDocumentId = uploadDocument(
                adminTokens,
                "Vacation Handbook",
                pdfWithPages(bestText)
        );
        UUID secondDocumentId = uploadDocument(
                adminTokens,
                "Benefits Guide",
                pdfWithPages(secondText)
        );
        processDocument(adminTokens, bestDocumentId);
        processDocument(adminTokens, secondDocumentId);

        mockMvc.perform(post("/api/knowledge/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", question,
                                "maxResults", 2
                        )))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(employeeTokens.accessToken())
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value(question))
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].documentId")
                        .value(bestDocumentId.toString()))
                .andExpect(jsonPath("$.results[0].documentName")
                        .value("Vacation Handbook"))
                .andExpect(jsonPath("$.results[0].pageNumber").value(1))
                .andExpect(jsonPath("$.results[0].content").value(bestText))
                .andExpect(jsonPath("$.results[0].similarity")
                        .value(closeTo(1.0, 0.0001)))
                .andExpect(jsonPath("$.results[1].documentId")
                        .value(secondDocumentId.toString()))
                .andExpect(jsonPath("$.results[1].similarity")
                        .value(closeTo(0.8, 0.0001)));
    }

    @Test
    void knowledgeSearchRequiresAuthenticationAndValidatesRequest() throws Exception {
        mockMvc.perform(post("/api/knowledge/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("question", "vacation policy"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        AuthenticationTokens employeeTokens = register(uniqueEmail());
        mockMvc.perform(post("/api/knowledge/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", " ",
                                "maxResults", 21
                        )))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(employeeTokens.accessToken())
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.question")
                        .value("Question is required"))
                .andExpect(jsonPath("$.fieldErrors.maxResults")
                        .value("Maximum results must be 20 or fewer"));
    }

    @Test
    void knowledgeSearchReturnsServiceUnavailableWhenEmbeddingFails() throws Exception {
        String question = "Where is the support policy?";
        when(embeddingModel.embed(eq(question))).thenThrow(
                new IllegalStateException("Simulated provider failure")
        );
        AuthenticationTokens employeeTokens = register(uniqueEmail());

        mockMvc.perform(post("/api/knowledge/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("question", question)))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(employeeTokens.accessToken())
                        ))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code")
                        .value("KNOWLEDGE_SEARCH_UNAVAILABLE"))
                .andExpect(jsonPath("$.message")
                        .value("Knowledge search is temporarily unavailable"));
    }

    @Test
    void authenticatedEmployeeReceivesGroundedAnswerWithDatabaseCitation()
            throws Exception {
        String sourceText = "Full-time employees receive twenty vacation days each year "
                + UUID.randomUUID();
        String question = "How many vacation days do full-time employees receive?";
        when(embeddingModel.embed(anyList())).thenAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream()
                    .map(input -> testEmbeddingVector(4, 1.0f, 5, 0.0f))
                    .toList();
        });
        when(embeddingModel.embed(eq(question)))
                .thenReturn(testEmbeddingVector(4, 1.0f, 5, 0.0f));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(
                "Full-time employees receive twenty vacation days each year [1]."
        ));
        AuthenticationTokens employeeTokens = register(uniqueEmail());
        AuthenticationTokens adminTokens = registerAdmin(uniqueEmail());
        UUID documentId = uploadDocument(
                adminTokens,
                "Employee Handbook",
                pdfWithPages(sourceText)
        );
        processDocument(adminTokens, documentId);

        mockMvc.perform(post("/api/knowledge/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", question,
                                "maxResults", 3
                        )))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(employeeTokens.accessToken())
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value(question))
                .andExpect(jsonPath("$.answer").value(
                        "Full-time employees receive twenty vacation days each year [1]."
                ))
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.citations.length()").value(1))
                .andExpect(jsonPath("$.citations[0].sourceNumber").value(1))
                .andExpect(jsonPath("$.citations[0].documentId")
                        .value(documentId.toString()))
                .andExpect(jsonPath("$.citations[0].documentName")
                        .value("Employee Handbook"))
                .andExpect(jsonPath("$.citations[0].pageNumber").value(1))
                .andExpect(jsonPath("$.citations[0].excerpt").value(containsString(
                        "Full-time employees receive twenty vacation days each year"
                )))
                .andExpect(jsonPath("$.citations[0].similarity")
                        .value(closeTo(1.0, 0.0001)));
    }

    @Test
    void knowledgeAnswerSkipsChatModelWhenRetrievalFindsNoSources() throws Exception {
        String question = "What is the interplanetary travel reimbursement policy?";
        when(embeddingModel.embed(eq(question)))
                .thenReturn(testEmbeddingVector(100, 1.0f, 101, 0.0f));
        AuthenticationTokens employeeTokens = register(uniqueEmail());

        mockMvc.perform(post("/api/knowledge/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("question", question)))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(employeeTokens.accessToken())
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(false))
                .andExpect(jsonPath("$.answer").value(
                        "I couldn't find enough information in the knowledge base "
                                + "to answer that question."
                ))
                .andExpect(jsonPath("$.citations").isEmpty());

        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void knowledgeAnswerReturnsServiceUnavailableWhenChatGenerationFails()
            throws Exception {
        String sourceText = "Password reset instructions " + UUID.randomUUID();
        String question = "How do I reset a customer password?";
        when(embeddingModel.embed(anyList())).thenAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream()
                    .map(input -> testEmbeddingVector(6, 1.0f, 7, 0.0f))
                    .toList();
        });
        when(embeddingModel.embed(eq(question)))
                .thenReturn(testEmbeddingVector(6, 1.0f, 7, 0.0f));
        when(chatModel.call(any(Prompt.class))).thenThrow(
                new IllegalStateException("Simulated chat provider failure")
        );
        AuthenticationTokens employeeTokens = register(uniqueEmail());
        AuthenticationTokens adminTokens = registerAdmin(uniqueEmail());
        UUID documentId = uploadDocument(
                adminTokens,
                "Support Manual",
                pdfWithPages(sourceText)
        );
        processDocument(adminTokens, documentId);

        mockMvc.perform(post("/api/knowledge/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("question", question)))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(employeeTokens.accessToken())
                        ))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code")
                        .value("KNOWLEDGE_ANSWER_UNAVAILABLE"))
                .andExpect(jsonPath("$.message")
                        .value("The knowledge answer could not be generated"));
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

    private UUID uploadDocument(
            AuthenticationTokens adminTokens,
            String displayName,
            byte[] content
    ) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/admin/documents")
                        .file(pdfFile("knowledge.pdf", content))
                        .param("displayName", displayName)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(adminTokens.accessToken())
                        ))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = new String(
                result.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8
        );
        return UUID.fromString(JsonPath.read(responseBody, "$.id"));
    }

    private void processDocument(
            AuthenticationTokens adminTokens,
            UUID documentId
    ) throws Exception {
        mockMvc.perform(post("/api/admin/documents/{documentId}/process", documentId)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(adminTokens.accessToken())
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    private long storedPdfCount() throws Exception {
        if (Files.notExists(DOCUMENT_STORAGE_DIRECTORY)) {
            return 0;
        }
        try (var storedFiles = Files.list(DOCUMENT_STORAGE_DIRECTORY)) {
            return storedFiles.filter(path -> path.toString().endsWith(".pdf")).count();
        }
    }

    private float[] testEmbeddingVector() {
        float[] vector = new float[1536];
        vector[0] = 1.0f;
        return vector;
    }

    private float[] testEmbeddingVector(
            int firstIndex,
            float firstValue,
            int secondIndex,
            float secondValue
    ) {
        float[] vector = new float[1536];
        vector[firstIndex] = firstValue;
        vector[secondIndex] = secondValue;
        return vector;
    }

    private ChatResponse chatResponse(String answer) {
        return new ChatResponse(List.of(
                new Generation(new AssistantMessage(answer))
        ));
    }

    private AuthenticationTokens login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        return readTokens(result);
    }

    private AuthenticationTokens registerAdmin(String email) throws Exception {
        register(email);
        User admin = userRepository.findByEmail(email).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);
        return login(email, PASSWORD);
    }

    private MockMultipartFile pdfFile(String filename, byte[] content) {
        return new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_PDF_VALUE,
                content
        );
    }

    private String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
        );
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

    private String json(Object body) {
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
