package com.concatenator.conatenate.controller;

import com.concatenator.conatenate.model.UserSettings;
import com.concatenator.conatenate.service.UserSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller for user settings management.
 *
 * Endpoints:
 * - GET /api/settings - Get all settings
 * - GET /api/settings/exclude-patterns - Get exclude patterns
 * - POST /api/settings/exclude-patterns - Add exclude pattern
 * - DELETE /api/settings/exclude-patterns/{pattern} - Remove exclude pattern
 * - POST /api/settings/include-extensions - Add include extension
 * - DELETE /api/settings/include-extensions/{ext} - Remove include extension
 * - PUT /api/settings/defaults - Update defaults
 * - POST /api/settings/reset - Reset to defaults
 */
@Slf4j
@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
public class SettingsController {

    private final UserSettingsService userSettingsService;

    public SettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    /**
     * Get all user settings.
     *
     * GET /api/settings
     *
     * @return UserSettings object
     */
    @GetMapping
    public ResponseEntity<UserSettings> getSettings() {
        log.info("Fetching user settings");
        UserSettings settings = userSettingsService.loadSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * Get all exclude patterns.
     *
     * GET /api/settings/exclude-patterns
     *
     * @return Set of exclude patterns
     */
    @GetMapping("/exclude-patterns")
    public ResponseEntity<?> getExcludePatterns() {
        UserSettings settings = userSettingsService.loadSettings();
        return ResponseEntity.ok(Map.of("excludePatterns", settings.getExcludePatterns()));
    }

    /**
     * Add a new exclude pattern.
     *
     * POST /api/settings/exclude-patterns
     * Body: { "pattern": "*.log" }
     *
     * @param request - Map containing pattern
     * @return Success message
     */
    @PostMapping("/exclude-patterns")
    public ResponseEntity<String> addExcludePattern(@RequestBody Map<String, String> request) {
        String pattern = request.get("pattern");

        if (pattern == null || pattern.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Pattern is required");
        }

        log.info("Adding exclude pattern: {}", pattern);
        userSettingsService.addExcludePattern(pattern);

        return ResponseEntity.ok("Exclude pattern added: " + pattern);
    }

    /**
     * Remove an exclude pattern.
     *
     * DELETE /api/settings/exclude-patterns/{pattern}
     *
     * @param pattern - Pattern to remove (URL encoded)
     * @return Success message
     */
    @DeleteMapping("/exclude-patterns/{pattern}")
    public ResponseEntity<String> removeExcludePattern(@PathVariable String pattern) {
        log.info("Removing exclude pattern: {}", pattern);
        userSettingsService.removeExcludePattern(pattern);
        return ResponseEntity.ok("Exclude pattern removed: " + pattern);
    }

    /**
     * Get all include extensions.
     *
     * GET /api/settings/include-extensions
     *
     * @return Set of include extensions
     */
    @GetMapping("/include-extensions")
    public ResponseEntity<?> getIncludeExtensions() {
        UserSettings settings = userSettingsService.loadSettings();
        return ResponseEntity.ok(Map.of("includeExtensions", settings.getIncludeExtensions()));
    }

    /**
     * Add a new include extension.
     *
     * POST /api/settings/include-extensions
     * Body: { "extension": ".java" }
     *
     * @param request - Map containing extension
     * @return Success message
     */
    @PostMapping("/include-extensions")
    public ResponseEntity<String> addIncludeExtension(@RequestBody Map<String, String> request) {
        String extension = request.get("extension");

        if (extension == null || extension.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Extension is required");
        }

        log.info("Adding include extension: {}", extension);
        userSettingsService.addIncludeExtension(extension);

        return ResponseEntity.ok("Include extension added: " + extension);
    }

    /**
     * Remove an include extension.
     *
     * DELETE /api/settings/include-extensions/{ext}
     *
     * @param ext - Extension to remove
     * @return Success message
     */
    @DeleteMapping("/include-extensions/{ext}")
    public ResponseEntity<String> removeIncludeExtension(@PathVariable String ext) {
        log.info("Removing include extension: {}", ext);
        userSettingsService.removeIncludeExtension(ext);
        return ResponseEntity.ok("Include extension removed: " + ext);
    }

    /**
     * Update default settings (output folder and max file size).
     *
     * PUT /api/settings/defaults
     * Body: {
     *   "outputFolder": "output",
     *   "maxFileSizeMb": 50
     * }
     *
     * @param request - Map with defaults
     * @return Success message
     */
    @PutMapping("/defaults")
    public ResponseEntity<String> updateDefaults(@RequestBody Map<String, Object> request) {
        String outputFolder = (String) request.get("outputFolder");
        Integer maxFileSizeMb = (Integer) request.get("maxFileSizeMb");

        log.info("Updating defaults: outputFolder={}, maxFileSizeMb={}",
                outputFolder, maxFileSizeMb);

        userSettingsService.updateDefaults(outputFolder, maxFileSizeMb);

        return ResponseEntity.ok("Defaults updated successfully");
    }

    /**
     * Reset settings to application defaults.
     *
     * POST /api/settings/reset
     *
     * @return Success message
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetSettings() {
        log.info("Resetting settings to defaults");
        userSettingsService.resetToDefaults();
        return ResponseEntity.ok("Settings reset to defaults");
    }
}
