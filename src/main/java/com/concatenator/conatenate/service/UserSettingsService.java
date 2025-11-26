package com.concatenator.conatenate.service;

import com.concatenator.conatenate.model.UserSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class UserSettingsService {

    // Jackson ObjectMapper for JSON serialization/deserialization
    private final ObjectMapper objectMapper;

    // Injected from application.properties
    @Value("${app.settings.file-path}")
    private String settingsFilePath;

    @Value("${app.default.exclude-patterns}")
    private String defaultExcludePatterns;

    @Value("${app.default.include-extensions}")
    private String defaultIncludeExtensions;

    @Value("${app.default.output-folder}")
    private String defaultOutputFolder;

    @Value("${app.default.max-file-size-mb}")
    private Integer defaultMaxFileSizeMb;

    public UserSettingsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Load user settings from JSON file in user's home directory.
     * If file doesn't exist, return default settings.
     *
     * Flow:
     * 1. Check if settings file exists (~/.project-concat-settings.json)
     * 2. If exists → read and parse JSON
     * 3. If not exists → create default settings
     * 4. Merge with application.properties defaults
     *
     * @return UserSettings object with all preferences
     */
    public UserSettings loadSettings() {
        Path settingsPath = Paths.get(settingsFilePath);

        if (Files.exists(settingsPath)) {
            try {
                // Read existing settings from JSON file
                log.info("Loading user settings from: {}", settingsFilePath);
                UserSettings settings = objectMapper.readValue(settingsPath.toFile(), UserSettings.class);
                log.debug("Loaded settings: {} exclude patterns, {} include extensions",
                        settings.getExcludePatterns().size(),
                        settings.getIncludeExtensions().size());
                return settings;
            } catch (IOException e) {
                log.error("Failed to load settings from {}, using defaults", settingsFilePath, e);
                return getDefaultSettings();
            }
        } else {
            // No settings file exists - create default
            log.info("No settings file found, creating default settings");
            UserSettings defaultSettings = getDefaultSettings();
            saveSettings(defaultSettings); // Save defaults for future use
            return defaultSettings;
        }
    }

    /**
     * Save user settings to JSON file.
     * Creates parent directories if they don't exist.
     *
     * @param settings - UserSettings object to save
     */
    public void saveSettings(UserSettings settings) {
        try {
            Path settingsPath = Paths.get(settingsFilePath);

            // Create parent directories if needed
            Files.createDirectories(settingsPath.getParent());

            // Update timestamp
            settings.setLastUpdated(System.currentTimeMillis());

            // Write settings to JSON file with pretty printing
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(settingsPath.toFile(), settings);

            log.info("Settings saved to: {}", settingsFilePath);

        } catch (IOException e) {
            log.error("Failed to save settings to {}", settingsFilePath, e);
            throw new RuntimeException("Failed to save user settings", e);
        }
    }

    /**
     * Get default settings from application.properties.
     * This is used when no user settings file exists.
     *
     * @return UserSettings with default values
     */
    private UserSettings getDefaultSettings() {
        return UserSettings.builder()
                .excludePatterns(parseCommaSeparated(defaultExcludePatterns))
                .includeExtensions(parseCommaSeparated(defaultIncludeExtensions))
                .defaultOutputFolder(defaultOutputFolder)
                .defaultMaxFileSizeMb(defaultMaxFileSizeMb)
                .lastUpdated(System.currentTimeMillis())
                .build();
    }

    /**
     * Parse comma-separated string into a Set.
     * Example: "*.java,*.py,*.xml" → Set("*.java", "*.py", "*.xml")
     *
     * @param commaSeparated - String with comma-separated values
     * @return Set of trimmed values
     */
    private Set<String> parseCommaSeparated(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.trim().isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(commaSeparated.split(",")));
    }

    /**
     * Add a custom exclude pattern to user settings.
     *
     * @param pattern - Pattern to exclude (e.g., "*.log", "temp/**")
     */
    public void addExcludePattern(String pattern) {
        UserSettings settings = loadSettings();
        settings.getExcludePatterns().add(pattern);
        saveSettings(settings);
        log.info("Added exclude pattern: {}", pattern);
    }

    /**
     * Remove an exclude pattern from user settings.
     *
     * @param pattern - Pattern to remove
     */
    public void removeExcludePattern(String pattern) {
        UserSettings settings = loadSettings();
        settings.getExcludePatterns().remove(pattern);
        saveSettings(settings);
        log.info("Removed exclude pattern: {}", pattern);
    }

    /**
     * Add an include extension to user settings.
     *
     * @param extension - Extension to include (e.g., ".java", ".py")
     */
    public void addIncludeExtension(String extension) {
        UserSettings settings = loadSettings();
        // Ensure extension starts with a dot
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        settings.getIncludeExtensions().add(extension);
        saveSettings(settings);
        log.info("Added include extension: {}", extension);
    }

    /**
     * Remove an include extension from user settings.
     *
     * @param extension - Extension to remove
     */
    public void removeIncludeExtension(String extension) {
        UserSettings settings = loadSettings();
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        settings.getIncludeExtensions().remove(extension);
        saveSettings(settings);
        log.info("Removed include extension: {}", extension);
    }

    /**
     * Update default preferences (output folder and max file size).
     *
     * @param outputFolder - Default output folder path
     * @param maxFileSizeMb - Default max file size in MB
     */
    public void updateDefaults(String outputFolder, Integer maxFileSizeMb) {
        UserSettings settings = loadSettings();
        if (outputFolder != null) {
            settings.setDefaultOutputFolder(outputFolder);
        }
        if (maxFileSizeMb != null) {
            settings.setDefaultMaxFileSizeMb(maxFileSizeMb);
        }
        saveSettings(settings);
        log.info("Updated defaults: outputFolder={}, maxFileSizeMb={}", outputFolder, maxFileSizeMb);
    }

    /**
     * Reset settings to defaults from application.properties.
     */
    public void resetToDefaults() {
        UserSettings defaultSettings = getDefaultSettings();
        saveSettings(defaultSettings);
        log.info("Settings reset to defaults");
    }
}
