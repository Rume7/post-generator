package com.codehacks.postgen.service;

import com.codehacks.postgen.model.Essay;
import com.codehacks.postgen.model.EssayStatus;
import com.codehacks.postgen.repository.EssayRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import com.codehacks.postgen.exception.EssayGenerationException;
import com.codehacks.postgen.exception.EssayServiceException;
import com.codehacks.postgen.exception.DuplicateEssayTopicException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EssayServiceImpl implements EssayService {

    private static final Logger logger = LoggerFactory.getLogger(EssayServiceImpl.class);
    
    // Business rules constants
    private static final int MIN_TOPIC_LENGTH = 3;
    private static final int MAX_TOPIC_LENGTH = 500;
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final String INVALID_TOPIC_CHARS_REGEX = "[<>\"'&]"; // Basic XSS prevention
    
    private final EssayRepository essayRepository;
    private final ChatClient chatClient;

    public EssayServiceImpl(EssayRepository essayRepository, ChatClient.Builder chatClientBuilder) {
        this.essayRepository = essayRepository;
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Essay generateAndSaveEssay(String topic) {
        // Validate input
        validateTopic(topic);
        
        // Check for duplicate topic
        if (essayRepository.existsByTopicIgnoreCase(topic)) {
            throw new DuplicateEssayTopicException("An essay with the topic '" + topic + "' already exists.");
        }
        
        try {
            String prompt = "Write a comprehensive essay on the topic: " + topic;
            logger.info("Generating essay for topic: {}", topic);
            
            String generatedContent = chatClient.prompt(prompt).call().content();
            
            // Validate generated content
            validateGeneratedContent(generatedContent);
            
            int lengthWords = calculateWordCount(generatedContent);
            
            Essay newEssay = Essay.builder()
                    .topic(topic)
                    .content(generatedContent)
                    .lengthWords(lengthWords)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .status(EssayStatus.DRAFT)
                    .build();

            Essay savedEssay = essayRepository.save(newEssay);
            logger.info("Successfully generated and saved essay with ID: {}", savedEssay.getId());
            return savedEssay;
            
        } catch (Exception e) {
            logger.error("Failed to generate essay for topic: {}", topic, e);
            throw new EssayGenerationException("Failed to generate essay for topic: " + topic, e);
        }
    }

    @Override
    public Optional<Essay> getEssayById(Long id) {
        if (id == null || id <= 0) {
            logger.warn("Invalid essay ID provided: {}", id);
            return Optional.empty();
        }
        
        try {
            return essayRepository.findById(id);
        } catch (Exception e) {
            logger.error("Error retrieving essay with ID: {}", id, e);
            throw new EssayServiceException("Failed to retrieve essay with ID: " + id, e);
        }
    }

    @Override
    public List<Essay> getAllEssays() {
        try {
            return essayRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all essays", e);
            throw new EssayServiceException("Failed to retrieve essays", e);
        }
    }

    @Override
    public Optional<Essay> updateEssay(Long id, Essay updatedEssay) {
        if (id == null || id <= 0) {
            logger.warn("Invalid essay ID provided for update: {}", id);
            return Optional.empty();
        }
        
        if (updatedEssay == null) {
            throw new IllegalArgumentException("Updated essay cannot be null");
        }
        
        // Validate updated essay data
        validateTopic(updatedEssay.getTopic());
        validateContent(updatedEssay.getContent());
        
        // Check for duplicate topic (excluding the current essay)
        Optional<Essay> existingWithTopic = essayRepository.findAll().stream()
            .filter(e -> e.getTopic().equalsIgnoreCase(updatedEssay.getTopic()) && !e.getId().equals(id))
            .findFirst();
        if (existingWithTopic.isPresent()) {
            throw new DuplicateEssayTopicException("An essay with the topic '" + updatedEssay.getTopic() + "' already exists.");
        }
        
        try {
            return essayRepository.findById(id).map(existingEssay -> {
                existingEssay.setTopic(updatedEssay.getTopic());
                existingEssay.setContent(updatedEssay.getContent());
                existingEssay.setLengthWords(calculateWordCount(updatedEssay.getContent()));
                existingEssay.setUpdatedAt(LocalDateTime.now());
                existingEssay.setStatus(updatedEssay.getStatus());
                
                Essay savedEssay = essayRepository.save(existingEssay);
                logger.info("Successfully updated essay with ID: {}", id);
                return savedEssay;
            });
        } catch (Exception e) {
            logger.error("Error updating essay with ID: {}", id, e);
            throw new EssayServiceException("Failed to update essay with ID: " + id, e);
        }
    }

    @Override
    public Optional<Essay> updateEssayStatus(Long id, EssayStatus newStatus) {
        if (id == null || id <= 0) {
            logger.warn("Invalid essay ID provided for status update: {}", id);
            return Optional.empty();
        }
        
        if (newStatus == null) {
            throw new EssayServiceException("New status cannot be null");
        }
        
        try {
            return essayRepository.findById(id).map(existingEssay -> {
                existingEssay.setStatus(newStatus);
                existingEssay.setUpdatedAt(LocalDateTime.now());
                
                Essay savedEssay = essayRepository.save(existingEssay);
                logger.info("Successfully updated status to {} for essay with ID: {}", newStatus, id);
                return savedEssay;
            });
        } catch (Exception e) {
            logger.error("Error updating status for essay with ID: {}", id, e);
            throw new EssayServiceException("Failed to update status for essay with ID: " + id, e);
        }
    }

    @Override
    public void deleteEssay(Long id) {
        if (id == null || id <= 0) {
            logger.warn("Invalid essay ID provided for deletion: {}", id);
            return;
        }
        
        try {
            if (essayRepository.existsById(id)) {
                essayRepository.deleteById(id);
                logger.info("Successfully deleted essay with ID: {}", id);
            } else {
                logger.warn("Attempted to delete non-existent essay with ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error deleting essay with ID: {}", id, e);
            throw new EssayServiceException("Failed to delete essay with ID: " + id, e);
        }
    }

    /**
     * Validates the topic input according to business rules.
     * @param topic The topic to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateTopic(String topic) {
        if (topic == null) {
            throw new IllegalArgumentException("Topic cannot be null");
        }
        
        String trimmedTopic = topic.trim();
        
        if (trimmedTopic.isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty or contain only whitespace");
        }
        
        if (trimmedTopic.length() < MIN_TOPIC_LENGTH) {
            throw new IllegalArgumentException("Topic must be at least " + MIN_TOPIC_LENGTH + " characters long");
        }
        
        if (trimmedTopic.length() > MAX_TOPIC_LENGTH) {
            throw new IllegalArgumentException("Topic cannot exceed " + MAX_TOPIC_LENGTH + " characters");
        }
        
        if (trimmedTopic.matches(".*" + INVALID_TOPIC_CHARS_REGEX + ".*")) {
            throw new IllegalArgumentException("Topic contains invalid characters");
        }
        
        // Check for common spam patterns
        if (trimmedTopic.toLowerCase().matches(".*(spam|viagra|casino|porn|xxx).*")) {
            throw new IllegalArgumentException("Topic contains inappropriate content");
        }
    }

    /**
     * Validates the generated content from AI service.
     * @param content The content to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateGeneratedContent(String content) {
        if (content == null) {
            throw new EssayGenerationException("AI service returned null content");
        }
        
        if (content.trim().isEmpty()) {
            throw new EssayGenerationException("AI service returned empty content");
        }
        
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new EssayGenerationException("Generated content exceeds maximum length of " + MAX_CONTENT_LENGTH + " characters");
        }
        
        // Check for minimum word count
        int wordCount = calculateWordCount(content);
        if (wordCount < 50) {
            throw new EssayGenerationException("Generated content is too short (minimum 50 words required)");
        }
    }

    /**
     * Validates essay content for updates.
     * @param content The content to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        
        if (content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty or contain only whitespace");
        }
        
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Content cannot exceed " + MAX_CONTENT_LENGTH + " characters");
        }
    }

    /**
     * Helper method to calculate approximate word count.
     * This remains a private helper method within the implementation.
     * @param text The text content to count words in.
     * @return The approximate number of words.
     */
    private int calculateWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return words.length;
    }
}
