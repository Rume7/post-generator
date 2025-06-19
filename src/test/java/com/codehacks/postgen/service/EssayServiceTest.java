package com.codehacks.postgen.service;

import com.codehacks.postgen.model.Essay;
import com.codehacks.postgen.model.EssayStatus;
import com.codehacks.postgen.repository.EssayRepository;
import com.codehacks.postgen.exception.EssayGenerationException;
import com.codehacks.postgen.exception.EssayServiceException;
import com.codehacks.postgen.exception.DuplicateEssayTopicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class EssayServiceTest {

    @Mock
    private EssayRepository essayRepository;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    private EssayServiceImpl essayService;

    @BeforeEach
    void setUp() {
        essayService = new EssayServiceImpl(essayRepository, chatClientBuilder);
    }


    @Test
    @DisplayName("Should generate and save a new essay with DRAFT status")
    void generateAndSaveEssay_Success() {
        // Mock the ChatClient chain for this test
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);
        String longMockContent = getContentForTesting();

        when(callResponseSpec.content()).thenReturn(longMockContent);

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        String topic = "The Future of AI";
        String expectedContent = getContentForTesting();
        int expectedWordCount = 73;

        when(essayRepository.save(any(Essay.class))).thenAnswer(invocation -> {
            Essay essay = invocation.getArgument(0);
            essay.setId(1L);
            return essay;
        });

        Essay savedEssay = testEssayService.generateAndSaveEssay(topic);

        assertNotNull(savedEssay.getId());
        assertEquals(topic, savedEssay.getTopic());
        assertEquals(expectedContent, savedEssay.getContent());
        assertEquals(expectedWordCount, savedEssay.getLengthWords());
        assertNotNull(savedEssay.getCreatedAt());
        assertNotNull(savedEssay.getUpdatedAt());
        assertEquals(EssayStatus.DRAFT, savedEssay.getStatus());

        verify(chatClient, times(1)).prompt(anyString());
        verify(essayRepository, times(1)).save(any(Essay.class));
    }

    private String getContentForTesting() {
        return """
                This is a comprehensive generated essay content with several words for testing purposes.
                The essay discusses various aspects of the topic in detail, providing insights and analysis that would be valuable for readers.
                Furthermore, it explores the implications of the topic in modern society, referencing historical context and future trends.
                The essay is structured to meet the minimum word count requirement for validation and serves as a robust test case for the service.
                """.replace("\n", " ").trim();
    }

    @Test
    @DisplayName("Should retrieve an essay by ID when it exists")
    void getEssayById_Found() {
        Long essayId = 1L;
        Essay existingEssay = Essay.builder()
                .id(essayId)
                .topic("Existing Topic")
                .content("Existing Content from the writeup")
                .lengthWords(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(EssayStatus.PUBLISHED)
                .build();

        when(essayRepository.findById(essayId)).thenReturn(Optional.of(existingEssay));

        Optional<Essay> foundEssay = essayService.getEssayById(essayId);

        assertTrue(foundEssay.isPresent());
        assertEquals(existingEssay, foundEssay.get());
        verify(essayRepository, times(1)).findById(essayId);
    }

    @Test
    @DisplayName("Should return empty Optional when essay by ID is not found")
    void getEssayById_NotFound() {
        Long essayId = 99L;
        when(essayRepository.findById(essayId)).thenReturn(Optional.empty());

        Optional<Essay> foundEssay = essayService.getEssayById(essayId);

        assertFalse(foundEssay.isPresent());
        verify(essayRepository, times(1)).findById(essayId);
    }

    @Test
    @DisplayName("Should retrieve all essays when some exist")
    void getAllEssays_Found() {
        List<Essay> essays = Arrays.asList(
                Essay.builder().id(1L).topic("Topic 1").content("Content 1").lengthWords(2).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).status(EssayStatus.DRAFT).build(),
                Essay.builder().id(2L).topic("Topic 2").content("Content 2").lengthWords(2).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).status(EssayStatus.PUBLISHED).build()
        );

        when(essayRepository.findAll()).thenReturn(essays);

        List<Essay> allEssays = essayService.getAllEssays();

        assertFalse(allEssays.isEmpty());
        assertEquals(2, allEssays.size());
        assertEquals(essays, allEssays);
        verify(essayRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no essays exist")
    void getAllEssays_Empty() {
        when(essayRepository.findAll()).thenReturn(Collections.emptyList());

        List<Essay> allEssays = essayService.getAllEssays();

        assertTrue(allEssays.isEmpty());
        verify(essayRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should update an existing essay successfully")
    void updateEssay_Success() {
        Long essayId = 1L;
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
        Essay existingEssay = Essay.builder()
                .id(essayId)
                .topic("Original Topic")
                .content("Original Content")
                .lengthWords(2)
                .createdAt(originalCreatedAt)
                .updatedAt(originalCreatedAt)
                .status(EssayStatus.DRAFT)
                .build();

        Essay updatedDetails = Essay.builder()
                .topic("Updated Topic for a new test case")
                .content("This is updated essay content with more words now.")
                .status(EssayStatus.PUBLISHED)
                .build();

        when(essayRepository.findById(essayId)).thenReturn(Optional.of(existingEssay));
        when(essayRepository.save(any(Essay.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Essay> result = essayService.updateEssay(essayId, updatedDetails);

        assertTrue(result.isPresent());
        Essay essay = result.get();
        assertEquals(essayId, essay.getId());
        assertEquals(updatedDetails.getTopic(), essay.getTopic());
        assertEquals(updatedDetails.getContent(), essay.getContent());
        assertEquals(9, essay.getLengthWords());
        assertEquals(originalCreatedAt, essay.getCreatedAt());
        assertTrue(essay.getUpdatedAt().isAfter(originalCreatedAt));
        assertEquals(updatedDetails.getStatus(), essay.getStatus());

        verify(essayRepository, times(1)).findById(essayId);
        verify(essayRepository, times(1)).save(existingEssay);
    }

    @Test
    @DisplayName("Should return empty Optional when updating a non-existent essay")
    void updateEssay_NotFound() {
        Long essayId = 9999L;
        Essay updatedDetails = Essay.builder()
                .topic("Non-existent topic")
                .content("Non-existent content")
                .status(EssayStatus.PUBLISHED)
                .build();

        when(essayRepository.findById(essayId)).thenReturn(Optional.empty());

        Optional<Essay> result = essayService.updateEssay(essayId, updatedDetails);

        assertFalse(result.isPresent());
        verify(essayRepository, times(1)).findById(essayId);
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should update essay status successfully")
    void updateEssayStatus_Success() {
        Long essayId = 1L;
        LocalDateTime originalUpdatedAt = LocalDateTime.now().minusDays(1);
        Essay existingEssay = Essay.builder()
                .id(essayId)
                .topic("Topic")
                .content("Content")
                .lengthWords(1)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(originalUpdatedAt)
                .status(EssayStatus.DRAFT)
                .build();

        when(essayRepository.findById(essayId)).thenReturn(Optional.of(existingEssay));
        when(essayRepository.save(any(Essay.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Essay> result = essayService.updateEssayStatus(essayId, EssayStatus.PUBLISHED);

        assertTrue(result.isPresent());
        Essay essay = result.get();
        assertEquals(EssayStatus.PUBLISHED, essay.getStatus());
        assertTrue(essay.getUpdatedAt().isAfter(originalUpdatedAt));

        verify(essayRepository, times(1)).findById(essayId);
        verify(essayRepository, times(1)).save(existingEssay);
    }

    @Test
    @DisplayName("Should return empty Optional when updating status of non-existent essay")
    void updateEssayStatus_NotFound() {
        Long essayId = 99L;
        when(essayRepository.findById(essayId)).thenReturn(Optional.empty());

        Optional<Essay> result = essayService.updateEssayStatus(essayId, EssayStatus.ARCHIVED);

        assertFalse(result.isPresent());
        verify(essayRepository, times(1)).findById(essayId);
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should delete an essay by ID")
    void deleteEssay_Success() {
        Long essayId = 1L;

        when(essayRepository.existsById(essayId)).thenReturn(true);
        doNothing().when(essayRepository).deleteById(essayId);

        essayService.deleteEssay(essayId);

        verify(essayRepository, times(1)).existsById(essayId);
        verify(essayRepository, times(1)).deleteById(essayId);
    }

    @Test
    @DisplayName("Should throw exception for null topic")
    void generateAndSaveEssay_NullTopic() {
        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        assertThrows(IllegalArgumentException.class,
                () -> testEssayService.generateAndSaveEssay(null)
        );

        verify(chatClient, never()).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should throw exception for empty topic")
    void generateAndSaveEssay_EmptyTopic() {
        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        assertThrows(IllegalArgumentException.class,
                () -> testEssayService.generateAndSaveEssay("")
        );

        verify(chatClient, never()).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should throw exception for whitespace-only topic")
    void generateAndSaveEssay_WhitespaceOnlyTopic() {
        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        assertThrows(IllegalArgumentException.class,
                () -> testEssayService.generateAndSaveEssay("   \t\n  ")
        );

        verify(chatClient, never()).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should throw exception for very long topic")
    void generateAndSaveEssay_VeryLongTopic() {
        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        // Create a very long topic (exceeds 500 characters)
        String veryLongTopic = "A".repeat(501);

        assertThrows(IllegalArgumentException.class,
                () -> testEssayService.generateAndSaveEssay(veryLongTopic)
        );

        verify(chatClient, never()).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should throw exception for single character topic")
    void generateAndSaveEssay_SingleCharacterTopic() {
        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        // Test with single character topic - should throw exception (minimum 3 characters)
        assertThrows(IllegalArgumentException.class,
                () -> testEssayService.generateAndSaveEssay("A")
        );

        verify(chatClient, never()).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should throw exception for topic with invalid characters")
    void generateAndSaveEssay_InvalidCharactersTopic() {
        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        assertThrows(IllegalArgumentException.class,
                () -> testEssayService.generateAndSaveEssay("AI <script>alert('xss')</script>")
        );

        verify(chatClient, never()).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should throw exception for topic with inappropriate content")
    void generateAndSaveEssay_InappropriateContentTopic() {
        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        assertThrows(IllegalArgumentException.class,
                () -> testEssayService.generateAndSaveEssay("Buy viagra now")
        );

        verify(chatClient, never()).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle valid topic with special characters")
    void generateAndSaveEssay_ValidSpecialCharactersTopic() {
        // Mock the ChatClient chain for this test
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);
        String longMockContent = getContentForTesting();

        when(callResponseSpec.content()).thenReturn(longMockContent);

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        when(essayRepository.save(any(Essay.class))).thenAnswer(invocation -> {
            Essay essay = invocation.getArgument(0);
            essay.setId(1L);
            return essay;
        });

        // Test with valid special characters (no XSS chars - removed &)
        String validSpecialTopic = "AI and Machine Learning: The Future of Technology! @#$%^()";

        Essay savedEssay = testEssayService.generateAndSaveEssay(validSpecialTopic);

        assertNotNull(savedEssay.getId());
        assertEquals(validSpecialTopic, savedEssay.getTopic());
        assertEquals("This is a comprehensive generated essay content with several words for testing purposes. The essay discusses various aspects of the topic in detail, providing insights and analysis that would be valuable for readers. Furthermore, it explores the implications of the topic in modern society, referencing historical context and future trends. The essay is structured to meet the minimum word count requirement for validation and serves as a robust test case for the service.", savedEssay.getContent());
        assertEquals(EssayStatus.DRAFT, savedEssay.getStatus());

        verify(chatClient, times(1)).prompt("Write a comprehensive essay on the topic: " + validSpecialTopic);
        verify(essayRepository, times(1)).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle minimum valid topic length")
    void generateAndSaveEssay_MinimumValidTopic() {
        // Mock the ChatClient chain for this test
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);
        String longMockContent = getContentForTesting();

        when(callResponseSpec.content()).thenReturn(longMockContent);

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        when(essayRepository.save(any(Essay.class))).thenAnswer(invocation -> {
            Essay essay = invocation.getArgument(0);
            essay.setId(1L);
            return essay;
        });

        // Test with minimum valid topic length (3 characters)
        String minimumTopic = "AIT";

        Essay savedEssay = testEssayService.generateAndSaveEssay(minimumTopic);

        assertNotNull(savedEssay.getId());
        assertEquals(minimumTopic, savedEssay.getTopic());
        assertEquals("This is a comprehensive generated essay content with several words for testing purposes. The essay discusses various aspects of the topic in detail, providing insights and analysis that would be valuable for readers. Furthermore, it explores the implications of the topic in modern society, referencing historical context and future trends. The essay is structured to meet the minimum word count requirement for validation and serves as a robust test case for the service.", savedEssay.getContent());
        assertEquals(EssayStatus.DRAFT, savedEssay.getStatus());

        verify(chatClient, times(1)).prompt("Write a comprehensive essay on the topic: " + minimumTopic);
        verify(essayRepository, times(1)).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle maximum valid topic length")
    void generateAndSaveEssay_MaximumValidTopic() {
        // Mock the ChatClient chain for this test
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);
        String longMockContent = getContentForTesting();

        when(callResponseSpec.content()).thenReturn(longMockContent);

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        when(essayRepository.save(any(Essay.class))).thenAnswer(invocation -> {
            Essay essay = invocation.getArgument(0);
            essay.setId(1L);
            return essay;
        });

        // Test with maximum valid topic length (500 characters)
        String maximumTopic = "A".repeat(500);

        Essay savedEssay = testEssayService.generateAndSaveEssay(maximumTopic);

        assertNotNull(savedEssay.getId());
        assertEquals(maximumTopic, savedEssay.getTopic());
        assertEquals("This is a comprehensive generated essay content with several words for testing purposes. The essay discusses various aspects of the topic in detail, providing insights and analysis that would be valuable for readers. Furthermore, it explores the implications of the topic in modern society, referencing historical context and future trends. The essay is structured to meet the minimum word count requirement for validation and serves as a robust test case for the service.", savedEssay.getContent());
        assertEquals(EssayStatus.DRAFT, savedEssay.getStatus());

        verify(chatClient, times(1)).prompt("Write a comprehensive essay on the topic: " + maximumTopic);
        verify(essayRepository, times(1)).save(any(Essay.class));
    }

    // ==============================================
    // AREA 2: ERROR HANDLING & EXCEPTION SCENARIOS
    // ==============================================

    @Test
    @DisplayName("Should handle AI service failure gracefully")
    void generateAndSaveEssay_AI_ServiceFailure() {
        // Mock the ChatClient chain for this test
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);

        // Simulate AI service failure
        when(callResponseSpec.content()).thenThrow(new RuntimeException("AI service unavailable"));

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        EssayGenerationException exception = assertThrows(
                EssayGenerationException.class,
                () -> testEssayService.generateAndSaveEssay("Valid Topic")
        );

        assertEquals("Failed to generate essay for topic: Valid Topic", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("AI service unavailable", exception.getCause().getMessage());

        verify(chatClient, times(1)).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle database save failure")
    void generateAndSaveEssay_DatabaseFailure() {
        // Mock the ChatClient chain for this test
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(getContentForTesting());

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        when(essayRepository.save(any(Essay.class))).thenThrow(new RuntimeException("Database connection failed"));

        EssayGenerationException exception = assertThrows(
                EssayGenerationException.class,
                () -> testEssayService.generateAndSaveEssay("Valid Topic")
        );

        assertEquals("Failed to generate essay for topic: Valid Topic", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Database connection failed", exception.getCause().getMessage());

        verify(chatClient, times(1)).prompt(anyString());
        verify(essayRepository, times(1)).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle null AI response content")
    void generateAndSaveEssay_NullAIResponse() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);

        // Simulate null AI response
        when(callResponseSpec.content()).thenReturn(null);

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        EssayGenerationException exception = assertThrows(
                EssayGenerationException.class,
                () -> testEssayService.generateAndSaveEssay("Valid Topic")
        );

        assertEquals("Failed to generate essay for topic: Valid Topic", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("AI service returned null content", exception.getCause().getMessage());

        verify(chatClient, times(1)).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle empty AI response content")
    void generateAndSaveEssay_EmptyAIResponse() {
        // Mock the ChatClient chain for this test
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);

        // Simulate empty AI response
        when(callResponseSpec.content()).thenReturn("");

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        EssayGenerationException exception = assertThrows(
                EssayGenerationException.class,
                () -> testEssayService.generateAndSaveEssay("Valid Topic")
        );

        assertEquals("Failed to generate essay for topic: Valid Topic", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("AI service returned empty content", exception.getCause().getMessage());

        verify(chatClient, times(1)).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle AI response content too short")
    void generateAndSaveEssay_ShortAIResponse() {
        // Mock the ChatClient chain for this test
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);

        // Simulate short AI response (less than 50 words)
        when(callResponseSpec.content()).thenReturn("This is a short response with only ten words.");

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        EssayGenerationException exception = assertThrows(
                EssayGenerationException.class,
                () -> testEssayService.generateAndSaveEssay("Valid Topic")
        );

        assertEquals("Failed to generate essay for topic: Valid Topic", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Generated content is too short (minimum 50 words required)", exception.getCause().getMessage());

        verify(chatClient, times(1)).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle AI response content too long")
    void generateAndSaveEssay_LongAIResponse() {
        // Mock the ChatClient chain for this test
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(requestSpec.call()).thenReturn(callResponseSpec);

        // Simulate very long AI response (exceeds 10,000 characters)
        String veryLongContent = "word ".repeat(10001); // 10,001 characters
        when(callResponseSpec.content()).thenReturn(veryLongContent);

        EssayServiceImpl testEssayService = new EssayServiceImpl(essayRepository, chatClientBuilder);

        EssayGenerationException exception = assertThrows(
                EssayGenerationException.class,
                () -> testEssayService.generateAndSaveEssay("Valid Topic")
        );

        assertEquals("Failed to generate essay for topic: Valid Topic", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Generated content exceeds maximum length of 10000 characters", exception.getCause().getMessage());

        verify(chatClient, times(1)).prompt(anyString());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle database findById failure")
    void getEssayById_DatabaseFailure() {
        when(essayRepository.findById(1L)).thenThrow(new EssayServiceException("Database connection failed"));

        EssayServiceException exception = assertThrows(
                EssayServiceException.class,
                () -> essayService.getEssayById(1L)
        );

        assertEquals("Failed to retrieve essay with ID: 1", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Database connection failed", exception.getCause().getMessage());

        verify(essayRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should handle database findAll failure")
    void getAllEssays_DatabaseFailure() {
        when(essayRepository.findAll()).thenThrow(new EssayServiceException("Database connection failed"));

        EssayServiceException exception = assertThrows(
                EssayServiceException.class,
                () -> essayService.getAllEssays()
        );

        assertEquals("Failed to retrieve essays", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Database connection failed", exception.getCause().getMessage());

        verify(essayRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle database update failure")
    void updateEssay_DatabaseFailure() {
        Essay existingEssay = Essay.builder()
                .id(1L)
                .topic("Original Topic")
                .content("Original content")
                .status(EssayStatus.DRAFT)
                .build();
        when(essayRepository.findById(1L)).thenReturn(Optional.of(existingEssay));

        when(essayRepository.save(any(Essay.class))).thenThrow(new EssayServiceException("Database connection failed"));

        Essay updatedEssay = Essay.builder()
                .topic("Updated Topic")
                .content("Updated content with enough words to meet the minimum requirement for validation purposes.")
                .status(EssayStatus.PUBLISHED)
                .build();

        EssayServiceException exception = assertThrows(
                EssayServiceException.class,
                () -> essayService.updateEssay(1L, updatedEssay)
        );

        assertEquals("Failed to update essay with ID: 1", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Database connection failed", exception.getCause().getMessage());

        verify(essayRepository, times(1)).findById(1L);
        verify(essayRepository, times(1)).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle database status update failure")
    void updateEssayStatus_DatabaseFailure() {
        Essay existingEssay = Essay.builder()
                .id(1L)
                .topic("Test Topic")
                .content("Test content")
                .status(EssayStatus.DRAFT)
                .build();
        when(essayRepository.findById(1L)).thenReturn(Optional.of(existingEssay));

        when(essayRepository.save(any(Essay.class))).thenThrow(new EssayServiceException("Database connection failed"));

        EssayServiceException exception = assertThrows(
                EssayServiceException.class,
                () -> essayService.updateEssayStatus(1L, EssayStatus.PUBLISHED)
        );

        assertEquals("Failed to update status for essay with ID: 1", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Database connection failed", exception.getCause().getMessage());

        verify(essayRepository, times(1)).findById(1L);
        verify(essayRepository, times(1)).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle database delete failure")
    void deleteEssay_DatabaseFailure() {
        when(essayRepository.existsById(1L)).thenReturn(true);

        doThrow(new EssayServiceException("Database connection failed")).when(essayRepository).deleteById(1L);

        EssayServiceException exception = assertThrows(
                EssayServiceException.class,
                () -> essayService.deleteEssay(1L)
        );

        assertEquals("Failed to delete essay with ID: 1", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Database connection failed", exception.getCause().getMessage());

        verify(essayRepository, times(1)).existsById(1L);
        verify(essayRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should handle null updated essay in updateEssay")
    void updateEssay_NullUpdatedEssay() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> essayService.updateEssay(1L, null)
        );

        assertEquals("Updated essay cannot be null", exception.getMessage());

        verify(essayRepository, never()).findById(anyLong());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle null status in updateEssayStatus")
    void updateEssayStatus_NullStatus() {
        EssayServiceException exception = assertThrows(
                EssayServiceException.class,
                () -> essayService.updateEssayStatus(1L, null)
        );

        assertEquals("New status cannot be null", exception.getMessage());

        verify(essayRepository, never()).findById(anyLong());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should handle invalid content in updateEssay")
    void updateEssay_InvalidContent() {
        Essay updatedEssay = Essay.builder()
                .topic("Valid Topic")
                .content("") // Empty content should fail validation
                .status(EssayStatus.PUBLISHED)
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> essayService.updateEssay(1L, updatedEssay)
        );

        assertEquals("Content cannot be empty or contain only whitespace", exception.getMessage());

        verify(essayRepository, never()).findById(anyLong());
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should not allow duplicate topic on essay creation")
    void generateAndSaveEssay_DuplicateTopic() {
        when(essayRepository.existsByTopicIgnoreCase("Duplicate Topic")).thenReturn(true);

        DuplicateEssayTopicException exception = assertThrows(
                DuplicateEssayTopicException.class,
                () -> essayService.generateAndSaveEssay("Duplicate Topic")
        );
        assertEquals("An essay with the topic 'Duplicate Topic' already exists.", exception.getMessage());
        verify(essayRepository, times(1)).existsByTopicIgnoreCase("Duplicate Topic");
        verify(essayRepository, never()).save(any(Essay.class));
    }

    @Test
    @DisplayName("Should not allow duplicate topic on essay update")
    void updateEssay_DuplicateTopic() {
        Essay updatedEssay = Essay.builder()
                .topic("Duplicate Topic")
                .content("Valid content for update.")
                .status(EssayStatus.PUBLISHED)
                .build();

        Essay existingEssay = Essay.builder()
                .id(2L)
                .topic("Duplicate Topic")
                .content("Other content.")
                .status(EssayStatus.DRAFT)
                .build();

        // Mock repository to return a list containing another essay with the same topic
        when(essayRepository.findAll()).thenReturn(List.of(existingEssay));

        DuplicateEssayTopicException exception = assertThrows(
                DuplicateEssayTopicException.class,
                () -> essayService.updateEssay(1L, updatedEssay)
        );

        assertEquals("An essay with the topic 'Duplicate Topic' already exists.", exception.getMessage());
        verify(essayRepository, times(1)).findAll();
        verify(essayRepository, never()).findById(anyLong());
        verify(essayRepository, never()).save(any(Essay.class));
    }
}