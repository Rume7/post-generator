package com.codehacks.postgen.controller;

import com.codehacks.postgen.dto.EssayRequest;
import com.codehacks.postgen.dto.EssayResponse;
import com.codehacks.postgen.dto.EssayUpdateStatusRequest;
import com.codehacks.postgen.dto.EssayFullUpdateRequest;
import com.codehacks.postgen.exception.EssayServiceException;
import com.codehacks.postgen.model.Essay;
import com.codehacks.postgen.service.EssayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping(EssayController.BASE_PATH)
@Tag(name = "Essays", description = "API for managing generated essays")
public class EssayController {

    private static final Logger logger = LoggerFactory.getLogger(EssayController.class);
    public static final String BASE_PATH = "/api/v1/essays";

    private final EssayService essayService;

    public EssayController(EssayService essayService) {
        this.essayService = essayService;
    }

    /**
     * Generates a new essay based on the provided topic and saves it.
     *
     * @param request The EssayRequest DTO containing the topic.
     * @return ResponseEntity with the created EssayResponse and HTTP status 201.
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate a new essay", description = "Generates an essay using an AI model and saves it with DRAFT status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Essay successfully generated and saved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EssayResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate topic",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error during essay generation or saving",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<EssayResponse> generateEssay(@Valid @RequestBody EssayRequest request) {
        logger.info("Received request to generate essay for topic: {}", request.getTopic());
        Essay newEssay = essayService.generateAndSaveEssay(request.getTopic());
        return new ResponseEntity<>(convertToDto(newEssay), HttpStatus.CREATED);
    }

    /**
     * Retrieves an essay by its ID.
     *
     * @param id The ID of the essay.
     * @return ResponseEntity with the EssayResponse and HTTP status 200, or 404 if not found.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get essay by ID", description = "Retrieves a single essay by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Essay found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EssayResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Essay not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<EssayResponse> getEssayById(@Parameter(description = "ID of the essay to retrieve") @PathVariable Long id) {
        logger.info("Received request to get essay with ID: {}", id);
        if (id == null || id <= 0) {
            logger.warn("Invalid essay ID: {}. Returning 404.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return essayService.getEssayById(id)
                .map(essay -> new ResponseEntity<>(convertToDto(essay), HttpStatus.OK))
                .orElseGet(() -> {
                    logger.warn("Essay with ID {} not found.", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    /**
     * Retrieves all essays.
     *
     * @return ResponseEntity with a list of EssayResponse objects and HTTP status 200.
     */
    @GetMapping
    @Operation(summary = "Get all essays", description = "Retrieves a list of all essays stored in the database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of essays retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EssayResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<EssayResponse>> getAllEssays() {
        logger.info("Received request to get all essays.");
        List<Essay> essays = essayService.getAllEssays();
        List<EssayResponse> essayResponses = essays.stream()
                .map(this::convertToDto)
                .toList();
        return new ResponseEntity<>(essayResponses, HttpStatus.OK);
    }

    /**
     * Updates an existing essay.
     *
     * @param id The ID of the essay to update.
     * @param request The EssayRequest DTO containing updated topic and content.
     * @return ResponseEntity with the updated EssayResponse and HTTP status 200, or 404 if not found.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing essay", description = "Updates the topic, content, and status of an existing essay.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Essay updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EssayResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate topic",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Essay not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<EssayResponse> updateEssay(
            @Parameter(description = "ID of the essay to update") @PathVariable Long id,
            @Valid @RequestBody EssayFullUpdateRequest request) {
        logger.info("Received request to update essay with ID: {}", id);
        if (id == null || id <= 0) {
            logger.warn("Invalid essay ID: {}. Returning 404.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // Map DTO to Entity for service layer
        // Note: The service methods handle validation and field updates like updatedAt
        Essay essayDetailsForUpdate = Essay.builder()
                .topic(request.getTopic())
                .content(request.getContent())
                .status(request.getStatus())
                .build();

        return essayService.updateEssay(id, essayDetailsForUpdate)
                .map(essay -> new ResponseEntity<>(convertToDto(essay), HttpStatus.OK))
                .orElseGet(() -> {
                    logger.warn("Essay with ID {} not found for update.", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }


    @PutMapping("/{id}/status")
    @Operation(summary = "Update essay status", description = "Updates the status of an existing essay (e.g., DRAFT to PUBLISHED).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Essay status updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EssayResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid ID or status supplied",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Essay not found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<EssayResponse> updateEssayStatus(
            @Parameter(description = "ID of the essay to update") @PathVariable Long id,
            @Valid @RequestBody EssayUpdateStatusRequest request) {
        logger.info("Received request to update status for essay ID: {} to {}", id, request.getStatus());
        if (id == null || id <= 0) {
            logger.warn("Invalid essay ID: {}. Returning 404.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return essayService.updateEssayStatus(id, request.getStatus())
                .map(essay -> new ResponseEntity<>(convertToDto(essay), HttpStatus.OK))
                .orElseGet(() -> {
                    logger.warn("Essay with ID {} not found for status update.", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    /**
     * Deletes an essay by its ID.
     *
     * @param id The ID of the essay to delete.
     * @return ResponseEntity with HTTP status 204 if successful, or 404 if not found.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an essay", description = "Deletes an essay by its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Essay deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Essay not found", // Service logs warning, doesn't throw 404, but API should reflect
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<String> deleteEssay(@Parameter(description = "ID of the essay to delete") @PathVariable Long id) {
        logger.info("Received request to delete essay with ID: {}", id);
        if (id == null || id <= 0) {
            logger.warn("Invalid essay ID: {}. Returning 404.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        try {
            if (essayService.getEssayById(id).isEmpty()) {
                logger.warn("Attempted to delete non-existent essay with ID: {}. Returning 404.", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            essayService.deleteEssay(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (EssayServiceException e) {
            logger.error("Error deleting essay with ID: {}", id, e);
            return new ResponseEntity<>("An internal essay service error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private EssayResponse convertToDto(Essay essay) {
        return EssayResponse.builder()
                .id(essay.getId())
                .topic(essay.getTopic())
                .content(essay.getContent())
                .lengthWords(essay.getLengthWords())
                .createdAt(essay.getCreatedAt())
                .updatedAt(essay.getUpdatedAt())
                .status(essay.getStatus())
                .build();
    }
}
