package com.codehacks.postgen.controller;

import com.codehacks.postgen.dto.EssayRequest;
import com.codehacks.postgen.dto.EssayFullUpdateRequest;
import com.codehacks.postgen.dto.EssayUpdateStatusRequest;
import com.codehacks.postgen.exception.DuplicateEssayTopicException;
import com.codehacks.postgen.exception.EssayServiceException;
import com.codehacks.postgen.exception.GlobalExceptionHandler;
import com.codehacks.postgen.model.Essay;
import com.codehacks.postgen.model.EssayStatus;
import com.codehacks.postgen.service.EssayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EssayControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EssayService essayService;

    @InjectMocks
    private EssayController essayController;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Essay sampleEssay;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(essayController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleEssay = Essay.builder()
                .id(1L)
                .topic("Test Topic")
                .content("Test Content for the essay generated.")
                .lengthWords(7)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(EssayStatus.DRAFT)
                .build();
    }

    // ========== POST /generate TESTS ==========

    @Test
    @DisplayName("POST: Should generate and save essay with 201 CREATED")
    void generateEssay_shouldReturnCreatedStatus() throws Exception {
        EssayRequest request = EssayRequest.builder().topic("New AI Essay").build();

        when(essayService.generateAndSaveEssay(anyString())).thenReturn(sampleEssay);

        mockMvc.perform(post(EssayController.BASE_PATH + "/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sampleEssay.getId()))
                .andExpect(jsonPath("$.topic").value(sampleEssay.getTopic()));

        verify(essayService, times(1)).generateAndSaveEssay(request.getTopic());
    }

    @Test
    @DisplayName("POST: Should return 400 BAD REQUEST for invalid topic")
    void generateEssay_shouldReturnBadRequestForInvalidTopic() throws Exception {
        EssayRequest request = EssayRequest.builder().topic("").build();

        mockMvc.perform(post(EssayController.BASE_PATH + "/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("topic: Topic is required")));

        verify(essayService, never()).generateAndSaveEssay(anyString());
    }

    @Test
    @DisplayName("POST: Should return 400 BAD REQUEST for null topic")
    void generateEssay_shouldReturnBadRequestForNullTopic() throws Exception {
        EssayRequest request = EssayRequest.builder().topic(null).build();

        mockMvc.perform(post(EssayController.BASE_PATH + "/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("topic: Topic is required")));

        verify(essayService, never()).generateAndSaveEssay(anyString());
    }

    @Test
    @DisplayName("POST: Should return 400 BAD REQUEST for whitespace-only topic")
    void generateEssay_shouldReturnBadRequestForWhitespaceTopic() throws Exception {
        EssayRequest request = EssayRequest.builder().topic("   ").build();

        mockMvc.perform(post(EssayController.BASE_PATH + "/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("topic: Topic is required")));

        verify(essayService, never()).generateAndSaveEssay(anyString());
    }

    @Test
    @DisplayName("POST: Should return 500 INTERNAL SERVER ERROR for malformed JSON")
    void generateEssay_shouldReturnInternalServerErrorForMalformedJson() throws Exception {
        String malformedJson = "{\"topic\": \"Test Topic\",}"; // Extra comma

        mockMvc.perform(post(EssayController.BASE_PATH + "/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isInternalServerError());

        verify(essayService, never()).generateAndSaveEssay(anyString());
    }

    @Test
    @DisplayName("POST: Should return 400 BAD REQUEST for missing topic field")
    void generateEssay_shouldReturnBadRequestForMissingTopicField() throws Exception {
        String jsonWithoutTopic = "{}";

        mockMvc.perform(post(EssayController.BASE_PATH + "/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithoutTopic))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("topic: Topic is required")));

        verify(essayService, never()).generateAndSaveEssay(anyString());
    }

    @Test
    @DisplayName("POST: Should return 500 INTERNAL SERVER ERROR for null request body")
    void generateEssay_shouldReturnInternalServerErrorForNullBody() throws Exception {
        mockMvc.perform(post(EssayController.BASE_PATH + "/generate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(essayService, never()).generateAndSaveEssay(anyString());
    }

    @Test
    @DisplayName("POST: Should return 409 CONFLICT for duplicate topic")
    void generateEssay_shouldReturnConflictForDuplicateTopic() throws Exception {
        EssayRequest request = EssayRequest.builder().topic("Existing Topic").build();

        when(essayService.generateAndSaveEssay(anyString()))
                .thenThrow(new DuplicateEssayTopicException("An essay with the topic 'Existing Topic' already exists."));

        mockMvc.perform(post(EssayController.BASE_PATH + "/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("An essay with the topic 'Existing Topic' already exists.")));

        verify(essayService, times(1)).generateAndSaveEssay(request.getTopic());
    }

    // ========== GET /{id} TESTS ==========

    @Test
    @DisplayName("GET: Should return essay by ID with 200 OK")
    void getEssayById_shouldReturnEssayWhenFound() throws Exception {
        Long essayId = 1L;
        when(essayService.getEssayById(essayId)).thenReturn(Optional.of(sampleEssay));

        mockMvc.perform(get(EssayController.BASE_PATH + "/{id}", essayId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleEssay.getId()))
                .andExpect(jsonPath("$.topic").value(sampleEssay.getTopic()));

        verify(essayService, times(1)).getEssayById(essayId);
    }

    @Test
    @DisplayName("GET: Should return 404 NOT FOUND when essay ID not found")
    void getEssayById_shouldReturnNotFoundWhenIdNotFound() throws Exception {
        Long essayId = 99L;
        when(essayService.getEssayById(essayId)).thenReturn(Optional.empty());

        mockMvc.perform(get(EssayController.BASE_PATH + "/{id}", essayId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(essayService, times(1)).getEssayById(essayId);
    }

    @Test
    @DisplayName("GET: Should return 404 NOT FOUND for negative ID")
    void getEssayById_shouldReturnNotFoundForNegativeId() throws Exception {
        mockMvc.perform(get(EssayController.BASE_PATH + "/{id}", -1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(essayService, never()).getEssayById(anyLong());
    }

    @Test
    @DisplayName("GET: Should return 404 NOT FOUND for zero ID")
    void getEssayById_shouldReturnNotFoundForZeroId() throws Exception {
        mockMvc.perform(get(EssayController.BASE_PATH + "/{id}", 0L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(essayService, never()).getEssayById(anyLong());
    }

    // ========== GET / TESTS ==========

    @Test
    @DisplayName("GET: Should return all essays with 200 OK")
    void getAllEssays_shouldReturnAllEssays() throws Exception {
        List<Essay> essays = List.of(sampleEssay, Essay.builder().id(2L).topic("Another").content("Content").build());
        when(essayService.getAllEssays()).thenReturn(essays);

        mockMvc.perform(get(EssayController.BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(sampleEssay.getId()));

        verify(essayService, times(1)).getAllEssays();
    }

    @Test
    @DisplayName("GET: Should return empty list when no essays exist")
    void getAllEssays_shouldReturnEmptyListWhenNoEssays() throws Exception {
        when(essayService.getAllEssays()).thenReturn(List.of());

        mockMvc.perform(get(EssayController.BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(essayService, times(1)).getAllEssays();
    }

    // ========== PUT /{id} TESTS ==========

    @Test
    @DisplayName("PUT: Should update essay with 200 OK")
    void updateEssay_shouldReturnOkStatus() throws Exception {
        Long essayId = 1L;
        EssayFullUpdateRequest request = EssayFullUpdateRequest.builder()
                .topic("Updated Topic")
                .content("Updated content here for the essay. This is a longer content that meets the minimum requirement of 50 characters for the essay content validation.")
                .status(EssayStatus.PUBLISHED)
                .build();

        Essay updatedEssay = Essay.builder()
                .id(essayId)
                .topic(request.getTopic())
                .content(request.getContent())
                .lengthWords(8)
                .createdAt(sampleEssay.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .status(request.getStatus())
                .build();

        when(essayService.updateEssay(anyLong(), any(Essay.class))).thenReturn(Optional.of(updatedEssay));

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}", essayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updatedEssay.getId()))
                .andExpect(jsonPath("$.topic").value(updatedEssay.getTopic()))
                .andExpect(jsonPath("$.status").value(updatedEssay.getStatus().name()));

        verify(essayService, times(1)).updateEssay(eq(essayId), any(Essay.class));
    }

    @Test
    @DisplayName("PUT: Should return 404 NOT FOUND when updating non-existent essay")
    void updateEssay_shouldReturnNotFoundWhenIdNotFound() throws Exception {
        Long essayId = 999L;
        EssayFullUpdateRequest request = EssayFullUpdateRequest.builder()
                .topic("Non-existent")
                .content("This is a longer content that meets the minimum requirement of 50 characters for the essay content validation.")
                .status(EssayStatus.DRAFT)
                .build();

        when(essayService.updateEssay(anyLong(), any(Essay.class))).thenReturn(Optional.empty());

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}", essayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(essayService, times(1)).updateEssay(eq(essayId), any(Essay.class));
    }

    @Test
    @DisplayName("PUT: Should return 400 BAD REQUEST for invalid content length (too short)")
    void updateEssay_shouldReturnBadRequestForInvalidContentLength() throws Exception {
        Long essayId = 1L;
        EssayFullUpdateRequest request = EssayFullUpdateRequest.builder()
                .topic("Valid Topic")
                .content("Too short") // Less than 50 characters
                .status(EssayStatus.DRAFT)
                .build();

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}", essayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("content")));

        verify(essayService, never()).updateEssay(anyLong(), any(Essay.class));
    }

    @Test
    @DisplayName("PUT: Should return 400 BAD REQUEST for null topic")
    void updateEssay_shouldReturnBadRequestForNullTopic() throws Exception {
        Long essayId = 1L;
        EssayFullUpdateRequest request = EssayFullUpdateRequest.builder()
                .topic(null)
                .content("This is a longer content that meets the minimum requirement of 50 characters for the essay content validation.")
                .status(EssayStatus.DRAFT)
                .build();

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}", essayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("topic")));

        verify(essayService, never()).updateEssay(anyLong(), any(Essay.class));
    }

    @Test
    @DisplayName("PUT: Should return 500 INTERNAL SERVER ERROR for malformed JSON")
    void updateEssay_shouldReturnInternalServerErrorForMalformedJson() throws Exception {
        Long essayId = 1L;
        String malformedJson = "{\"topic\": \"Test\", \"content\": \"Valid content\",}"; // Extra comma

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}", essayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isInternalServerError());

        verify(essayService, never()).updateEssay(anyLong(), any(Essay.class));
    }

    @Test
    @DisplayName("PUT: Should return 404 NOT FOUND for negative ID")
    void updateEssay_shouldReturnNotFoundForNegativeId() throws Exception {
        EssayFullUpdateRequest request = EssayFullUpdateRequest.builder()
                .topic("Valid Topic")
                .content("This is a longer content that meets the minimum requirement of 50 characters for the essay content validation.")
                .status(EssayStatus.DRAFT)
                .build();

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}", -1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(essayService, never()).updateEssay(anyLong(), any(Essay.class));
    }

    // ========== PUT /{id}/status TESTS ==========

    @Test
    @DisplayName("PUT: Should update essay status with 200 OK")
    void updateEssayStatus_shouldReturnOkStatus() throws Exception {
        Long essayId = 1L;
        EssayUpdateStatusRequest request = EssayUpdateStatusRequest.builder()
                .status(EssayStatus.ARCHIVED)
                .build();

        Essay updatedEssay = Essay.builder()
                .id(essayId)
                .topic(sampleEssay.getTopic())
                .content(sampleEssay.getContent())
                .lengthWords(sampleEssay.getLengthWords())
                .createdAt(sampleEssay.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .status(request.getStatus())
                .build();

        when(essayService.updateEssayStatus(anyLong(), any(EssayStatus.class))).thenReturn(Optional.of(updatedEssay));

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}/status", essayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updatedEssay.getId()))
                .andExpect(jsonPath("$.status").value(updatedEssay.getStatus().name()));

        verify(essayService, times(1)).updateEssayStatus(eq(essayId), eq(request.getStatus()));
    }

    @Test
    @DisplayName("PUT: Should return 404 NOT FOUND when updating status of non-existent essay")
    void updateEssayStatus_shouldReturnNotFoundWhenIdNotFound() throws Exception {
        Long essayId = 999L;
        EssayUpdateStatusRequest request = EssayUpdateStatusRequest.builder()
                .status(EssayStatus.ARCHIVED)
                .build();

        when(essayService.updateEssayStatus(anyLong(), any(EssayStatus.class))).thenReturn(Optional.empty());

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}/status", essayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(essayService, times(1)).updateEssayStatus(eq(essayId), eq(request.getStatus()));
    }

    @Test
    @DisplayName("PUT: Should return 400 BAD REQUEST for null status")
    void updateEssayStatus_shouldReturnBadRequestForNullStatus() throws Exception {
        Long essayId = 1L;
        EssayUpdateStatusRequest request = EssayUpdateStatusRequest.builder()
                .status(null)
                .build();

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}/status", essayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("status")));

        verify(essayService, never()).updateEssayStatus(anyLong(), any(EssayStatus.class));
    }

    @Test
    @DisplayName("PUT: Should return 500 INTERNAL SERVER ERROR for invalid status value")
    void updateEssayStatus_shouldReturnInternalServerErrorForInvalidStatus() throws Exception {
        Long essayId = 1L;
        String invalidStatusJson = "{\"status\": \"INVALID_STATUS\"}";

        mockMvc.perform(put(EssayController.BASE_PATH + "/{id}/status", essayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidStatusJson))
                .andExpect(status().isInternalServerError());

        verify(essayService, never()).updateEssayStatus(anyLong(), any(EssayStatus.class));
    }

    // ========== DELETE /{id} TESTS ==========

    @Test
    @DisplayName("DELETE: Should delete essay with 204 NO CONTENT")
    void deleteEssay_shouldReturnNoContentOnSuccess() throws Exception {
        Long essayId = 1L;

        when(essayService.getEssayById(essayId)).thenReturn(Optional.of(sampleEssay));
        doNothing().when(essayService).deleteEssay(essayId);

        mockMvc.perform(delete(EssayController.BASE_PATH + "/{id}", essayId))
                .andExpect(status().isNoContent());

        verify(essayService, times(1)).getEssayById(essayId);
        verify(essayService, times(1)).deleteEssay(essayId);
    }

    @Test
    @DisplayName("DELETE: Should return 404 NOT FOUND when essay does not exist")
    void deleteEssay_shouldReturnNotFoundWhenEssayDoesNotExist() throws Exception {
        Long essayId = 99L;

        when(essayService.getEssayById(essayId)).thenReturn(Optional.empty());

        mockMvc.perform(delete(EssayController.BASE_PATH + "/{id}", essayId))
                .andExpect(status().isNotFound());

        verify(essayService, times(1)).getEssayById(essayId);
        verify(essayService, never()).deleteEssay(anyLong());
    }

    @Test
    @DisplayName("DELETE: Should return 500 INTERNAL SERVER ERROR on service exception")
    void deleteEssay_shouldReturnInternalServerErrorOnServiceException() throws Exception {
        Long essayId = 1L;

        when(essayService.getEssayById(essayId)).thenReturn(Optional.of(sampleEssay));

        doThrow(new EssayServiceException("Database error during deletion")).when(essayService).deleteEssay(essayId);

        mockMvc.perform(delete(EssayController.BASE_PATH + "/{id}", essayId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("An internal essay service error occurred: Database error during deletion")));

        verify(essayService, times(1)).getEssayById(essayId);
        verify(essayService, times(1)).deleteEssay(essayId);
    }

    @Test
    @DisplayName("DELETE: Should return 404 NOT FOUND for negative ID")
    void deleteEssay_shouldReturnNotFoundForNegativeId() throws Exception {
        mockMvc.perform(delete(EssayController.BASE_PATH + "/{id}", -1L))
                .andExpect(status().isNotFound());

        verify(essayService, never()).getEssayById(anyLong());
        verify(essayService, never()).deleteEssay(anyLong());
    }

    @Test
    @DisplayName("DELETE: Should return 404 NOT FOUND for zero ID")
    void deleteEssay_shouldReturnNotFoundForZeroId() throws Exception {
        mockMvc.perform(delete(EssayController.BASE_PATH + "/{id}", 0L))
                .andExpect(status().isNotFound());

        verify(essayService, never()).getEssayById(anyLong());
        verify(essayService, never()).deleteEssay(anyLong());
    }
}
