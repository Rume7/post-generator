package com.codehacks.postgen;

import com.codehacks.postgen.controller.EssayController;
import com.codehacks.postgen.dto.EssayRequest;
import com.codehacks.postgen.dto.EssayResponse;
import com.codehacks.postgen.dto.EssayFullUpdateRequest;
import com.codehacks.postgen.dto.EssayUpdateStatusRequest;
import com.codehacks.postgen.model.EssayStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.ai.vertexai.gemini.enabled=false",
        "spring.ai.gemini.enabled=false"
    },
    classes = {PostGeneratorApplication.class, TestConfig.class}
)
@TestPropertySource(properties = {
    "spring.ai.vertexai.gemini.enabled=false",
    "spring.ai.gemini.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.ai.model.vertexai.autoconfigure.gemini.VertexAiGeminiChatAutoConfiguration"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EssayE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpassword");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false"); // Good practice for REST APIs

        // Disable Spring AI for integration tests
        registry.add("spring.ai.vertexai.gemini.enabled", () -> "false");
        registry.add("spring.ai.gemini.enabled", () -> "false");
        
        // Set test profile
        registry.add("spring.profiles.active", () -> "test");
    }

    // Using a shared EssayResponse to avoid re-creating in multiple tests
    private static Long generatedEssayId;

    @Test
    @Order(1)
    @DisplayName("E2E: 1. Should generate and save an essay (POST /generate)")
    void testGenerateEssay() {
        EssayRequest generateRequest = EssayRequest.builder().topic("The Future of Green Energy").build();

        ResponseEntity<EssayResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + EssayController.BASE_PATH + "/generate",
                generateRequest,
                EssayResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getTopic()).isEqualTo(generateRequest.getTopic());
        assertThat(response.getBody().getContent()).isNotBlank();
        assertThat(response.getBody().getLengthWords()).isGreaterThanOrEqualTo(50); // From service validation
        assertThat(response.getBody().getCreatedAt()).isNotNull();
        assertThat(response.getBody().getUpdatedAt()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(EssayStatus.DRAFT);

        generatedEssayId = response.getBody().getId();
        System.out.println("Generated Essay ID: " + generatedEssayId);
    }

    @Test
    @Order(2)
    @DisplayName("E2E: 2. Should retrieve the generated essay by ID (GET /{id})")
    void testGetEssayById() {
        assertThat(generatedEssayId).isNotNull();

        ResponseEntity<EssayResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + EssayController.BASE_PATH + "/" + generatedEssayId,
                EssayResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(generatedEssayId);
        assertThat(response.getBody().getTopic()).isEqualTo("The Future of Green Energy");
        assertThat(response.getBody().getContent()).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("E2E: 3. Should retrieve all essays (GET /)")
    void testGetAllEssays() {
        // Assuming testGenerateEssay has run and populated at least one essay
        ResponseEntity<List> response = restTemplate.getForEntity(
                "http://localhost:" + port + EssayController.BASE_PATH,
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Order(4)
    @DisplayName("E2E: 4. Should update an existing essay (PUT /{id})")
    void testUpdateEssay() {
        assertThat(generatedEssayId).isNotNull();

        EssayFullUpdateRequest updateRequest = EssayFullUpdateRequest.builder()
                .topic("Revised Green Energy Outlook")
                .content("This is the fully revised content of the green energy essay. It now includes more details and updated insights. This content needs to be long enough to meet the minimum word count validation of the service layer.")
                .status(EssayStatus.PUBLISHED)
                .build();

        // Use restTemplate.exchange to get the response from PUT
        ResponseEntity<EssayResponse> putResponse = restTemplate.exchange(
                "http://localhost:" + port + EssayController.BASE_PATH + "/" + generatedEssayId,
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(updateRequest),
                EssayResponse.class);

        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(putResponse.getBody()).isNotNull();
        assertThat(putResponse.getBody().getId()).isEqualTo(generatedEssayId);
        assertThat(putResponse.getBody().getTopic()).isEqualTo(updateRequest.getTopic());
        assertThat(putResponse.getBody().getContent()).isEqualTo(updateRequest.getContent());
        assertThat(putResponse.getBody().getStatus()).isEqualTo(updateRequest.getStatus());
        assertThat(putResponse.getBody().getUpdatedAt()).isAfter(putResponse.getBody().getCreatedAt()); // Updated timestamp check
    }

    @Test
    @Order(5)
    @DisplayName("E2E: 5. Should update essay status (PUT /{id}/status)")
    void testUpdateEssayStatus() {
        assertThat(generatedEssayId).isNotNull();

        EssayUpdateStatusRequest statusRequest = EssayUpdateStatusRequest.builder()
                .status(EssayStatus.ARCHIVED)
                .build();

        ResponseEntity<EssayResponse> putResponse = restTemplate.exchange(
                "http://localhost:" + port + EssayController.BASE_PATH + "/" + generatedEssayId + "/status",
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(statusRequest),
                EssayResponse.class);

        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(putResponse.getBody()).isNotNull();
        assertThat(putResponse.getBody().getId()).isEqualTo(generatedEssayId);
        assertThat(putResponse.getBody().getStatus()).isEqualTo(statusRequest.getStatus());
        assertThat(putResponse.getBody().getUpdatedAt()).isAfter(putResponse.getBody().getCreatedAt()); // Updated timestamp check
    }

    @Test
    @Order(6)
    @DisplayName("E2E: 6. Should delete the essay (DELETE /{id})")
    void testDeleteEssay() {
        assertThat(generatedEssayId).isNotNull();

        restTemplate.delete("http://localhost:" + port + EssayController.BASE_PATH + "/" + generatedEssayId);

        // Verify deletion by trying to retrieve, expecting 404
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + EssayController.BASE_PATH + "/" + generatedEssayId,
                String.class); // Use String.class as body will be an error message or empty

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- Edge Cases and Error Scenarios ---

    @Test
    @Order(7)
    @DisplayName("E2E: Should return 400 for invalid topic on essay generation")
    void testGenerateEssay_invalidTopic() {
        EssayRequest invalidRequest = EssayRequest.builder().topic("").build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + EssayController.BASE_PATH + "/generate",
                invalidRequest,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("topic: Topic is required");
    }

    @Test
    @Order(8)
    @DisplayName("E2E: Should return 409 for duplicate topic on essay generation")
    void testGenerateEssay_duplicateTopic() {
        // First, generate an essay with a unique topic
        EssayRequest initialRequest = EssayRequest.builder().topic("UniqueTopicForDuplicateTest").build();

        ResponseEntity<EssayResponse> initialResponse = restTemplate.postForEntity(
                "http://localhost:" + port + EssayController.BASE_PATH + "/generate",
                initialRequest,
                EssayResponse.class);
        assertThat(initialResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Try to generate another essay with the exact same topic
        ResponseEntity<String> duplicateResponse = restTemplate.postForEntity(
                "http://localhost:" + port + EssayController.BASE_PATH + "/generate",
                initialRequest, String.class);

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateResponse.getBody()).contains("An essay with the topic 'UniqueTopicForDuplicateTest' already exists.");
    }

    @Test
    @Order(9)
    @DisplayName("E2E: Should return 404 when retrieving a non-existent essay")
    void testGetEssayById_notFound() {
        long nonExistentId = 99999L;

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + EssayController.BASE_PATH + "/" + nonExistentId,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(10)
    @DisplayName("E2E: Should return 404 when updating a non-existent essay")
    void testUpdateEssay_notFound() {
        long nonExistentId = 99999L;

        EssayFullUpdateRequest updateRequest = EssayFullUpdateRequest.builder()
                .topic("Non-existent Topic")
                .content("This is a longer content for the non-existent essay update that meets the minimum length requirement of 50 characters.")
                .status(EssayStatus.DRAFT)
                .build();

        ResponseEntity<String> putResponse = restTemplate.exchange(
                "http://localhost:" + port + EssayController.BASE_PATH + "/" + nonExistentId,
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(updateRequest),
                String.class);

        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(11)
    @DisplayName("E2E: Should return 404 when deleting a non-existent essay")
    void testDeleteEssay_notFound() {
        long nonExistentId = 99997L;

        restTemplate.delete("http://localhost:" + port + EssayController.BASE_PATH + "/" + nonExistentId);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + EssayController.BASE_PATH + "/" + nonExistentId,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
