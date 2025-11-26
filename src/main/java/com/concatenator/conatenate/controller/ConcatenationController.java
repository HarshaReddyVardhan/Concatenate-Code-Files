package com.concatenator.conatenate.controller;

import com.concatenator.conatenate.model.ConcatenationRequest;
import com.concatenator.conatenate.model.ConcatenationResult;
import com.concatenator.conatenate.service.ConcatenationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller for project concatenation operations.
 *
 * Endpoints:
 * - POST /api/generate - Full project scan and concatenation
 * - POST /api/update - Incremental update (changed files only)
 * - GET /api/health - Health check
 */
@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ConcatenationController {

    private final ConcatenationService concatenationService;

    public ConcatenationController(ConcatenationService concatenationService) {
        this.concatenationService = concatenationService;
    }

    /**
     * Generate full concatenation for a project.
     *
     * Example Request:
     * POST /api/generate
     * {
     *   "projectPath": "/path/to/project",
     *   "outputFolder": "output",
     *   "maxFileSizeMb": 30
     * }
     *
     * @param request - Concatenation configuration
     * @return ConcatenationResult with statistics and file paths
     */
    @PostMapping("/generate")
    public ResponseEntity<ConcatenationResult> generateConcatenation(
            @Valid @RequestBody ConcatenationRequest request) {

        log.info("Received concatenation request for: {}", request.getProjectPath());

        // Set incremental to false for full generation
        request.setIncrementalUpdate(false);

        ConcatenationResult result = concatenationService.generateConcatenation(request);

        if (result.getSuccess()) {
            log.info("Concatenation successful: {} files processed",
                    result.getTotalFilesProcessed());
            return ResponseEntity.ok(result);
        } else {
            log.error("Concatenation failed: {}", result.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Perform incremental update (process only changed files).
     *
     * Example Request:
     * POST /api/update
     * {
     *   "projectPath": "/path/to/project"
     * }
     *
     * @param request - Concatenation configuration
     * @return ConcatenationResult with statistics
     */
    @PostMapping("/update")
    public ResponseEntity<ConcatenationResult> incrementalUpdate(
            @Valid @RequestBody ConcatenationRequest request) {

        log.info("Received incremental update request for: {}", request.getProjectPath());

        // Set incremental to true
        request.setIncrementalUpdate(true);

        ConcatenationResult result = concatenationService.generateConcatenation(request);

        if (result.getSuccess()) {
            log.info("Incremental update successful: {} files changed, {} skipped",
                    result.getFilesChanged(), result.getFilesSkipped());
            return ResponseEntity.ok(result);
        } else {
            log.error("Incremental update failed: {}", result.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Health check endpoint.
     *
     * GET /api/health
     *
     * @return Simple status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Project Concatenator API is running");
    }
}
