package com.concatenator.conatenate.service;

import com.concatenator.conatenate.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConcatenationService {

    private final FileHashService fileHashService;
    private final UserSettingsService userSettingsService;
    private final ObjectMapper objectMapper;

    // Constants
    private static final String METADATA_FILE_NAME = ".project-concat-metadata.json";
    private static final String STRUCTURE_FILE_NAME = "PROJECT_STRUCTURE.json";
    private static final String FILE_SEPARATOR = "=== %s ===\n";
    private static final long MB_TO_BYTES = 1024 * 1024;

    public ConcatenationService(FileHashService fileHashService,
                                UserSettingsService userSettingsService,
                                ObjectMapper objectMapper) {
        this.fileHashService = fileHashService;
        this.userSettingsService = userSettingsService;
        this.objectMapper = objectMapper;
    }

    /**
     * MAIN METHOD: Generate concatenated files for a project.
     *
     * Process Flow:
     * 1. Validate input and merge settings
     * 2. Scan project directory for files
     * 3. Filter files based on patterns/extensions
     * 4. Group files by top-level folder
     * 5. Generate concatenated files (split if needed)
     * 6. Generate PROJECT_STRUCTURE.json
     * 7. Save metadata for future incremental updates
     *
     * @param request - ConcatenationRequest with project path and settings
     * @return ConcatenationResult with statistics and output file paths
     */
    public ConcatenationResult generateConcatenation(ConcatenationRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Starting concatenation for project: {}", request.getProjectPath());

        try {
            // Step 1: Validate and prepare
            Path projectPath = Paths.get(request.getProjectPath());
            if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
                return createErrorResult("Project path does not exist or is not a directory");
            }

            // Step 2: Merge user settings with request
            UserSettings userSettings = userSettingsService.loadSettings();
            Set<String> excludePatterns = mergeSettings(
                    request.getExcludePatterns(),
                    userSettings.getExcludePatterns()
            );
            Set<String> includeExtensions = mergeSettings(
                    request.getIncludeExtensions(),
                    userSettings.getIncludeExtensions()
            );

            String outputFolder = request.getOutputFolder() != null
                    ? request.getOutputFolder()
                    : userSettings.getDefaultOutputFolder();

            int maxFileSizeMb = request.getMaxFileSizeMb() != null
                    ? request.getMaxFileSizeMb()
                    : userSettings.getDefaultMaxFileSizeMb();

            // Step 3: Load previous metadata (for incremental updates)
            ProjectMetadata previousMetadata = loadMetadata(projectPath);
            boolean isIncremental = Boolean.TRUE.equals(request.getIncrementalUpdate())
                    && previousMetadata != null;

            // Step 4: Scan project files
            log.info("Scanning project files...");
            List<Path> allFiles = scanProjectFiles(projectPath, excludePatterns, includeExtensions);
            log.info("Found {} files to process", allFiles.size());

            // Step 5: Calculate hashes and detect changes
            Map<String, FileMetadata> currentMetadata = new HashMap<>();
            List<Path> filesToProcess = new ArrayList<>();
            int filesSkipped = 0;

            for (Path file : allFiles) {
                String relativePath = projectPath.relativize(file).toString();
                String hash = fileHashService.calculateFileHash(file);

                FileMetadata metadata = FileMetadata.builder()
                        .filePath(relativePath)
                        .sha256Hash(hash)
                        .lastModified(Files.getLastModifiedTime(file).toMillis())
                        .fileSize(Files.size(file))
                        .build();

                currentMetadata.put(relativePath, metadata);

                // Check if file changed (for incremental updates)
                if (isIncremental && previousMetadata.getFiles().containsKey(relativePath)) {
                    String previousHash = previousMetadata.getFiles().get(relativePath).getSha256Hash();
                    if (hash.equals(previousHash)) {
                        filesSkipped++;
                        continue; // File unchanged, skip it
                    }
                }

                filesToProcess.add(file);
            }

            log.info("Files to process: {}, Files skipped: {}", filesToProcess.size(), filesSkipped);

            // Step 6: Group files by top-level folder
            Map<String, List<Path>> filesByFolder = groupFilesByTopLevelFolder(
                    projectPath,
                    filesToProcess
            );

            // Step 7: Create output directory
            Path outputPath = projectPath.resolve(outputFolder);
            Files.createDirectories(outputPath);

            // Step 8: Generate concatenated files for each folder
            List<String> outputFiles = new ArrayList<>();
            long totalSize = 0;

            for (Map.Entry<String, List<Path>> entry : filesByFolder.entrySet()) {
                String folderName = entry.getKey();
                List<Path> files = entry.getValue();

                List<String> generatedFiles = concatenateFilesWithSplit(
                        files,
                        projectPath,
                        outputPath,
                        folderName,
                        maxFileSizeMb
                );

                outputFiles.addAll(generatedFiles);
            }

            // Step 9: Generate PROJECT_STRUCTURE.json
            String structureFile = generateProjectStructure(projectPath, allFiles, outputPath);

            // Step 10: Save metadata
            ProjectMetadata newMetadata = ProjectMetadata.builder()
                    .projectPath(projectPath.toString())
                    .lastScanTime(System.currentTimeMillis())
                    .files(currentMetadata)
                    .totalFiles(allFiles.size())
                    .totalSize(currentMetadata.values().stream()
                            .mapToLong(FileMetadata::getFileSize)
                            .sum())
                    .build();

            String metadataFile = saveMetadata(projectPath, newMetadata);

            // Step 11: Calculate total output size
            for (String file : outputFiles) {
                totalSize += Files.size(Paths.get(file));
            }

            long processingTime = System.currentTimeMillis() - startTime;

            log.info("Concatenation completed in {}ms", processingTime);

            return ConcatenationResult.builder()
                    .success(true)
                    .message("Concatenation completed successfully")
                    .outputFiles(outputFiles)
                    .totalFilesProcessed(filesToProcess.size())
                    .filesChanged(filesToProcess.size())
                    .filesSkipped(filesSkipped)
                    .totalSizeBytes(totalSize)
                    .processingTimeMs(processingTime)
                    .projectStructureFile(structureFile)
                    .metadataFile(metadataFile)
                    .build();

        } catch (Exception e) {
            log.error("Error during concatenation", e);
            return createErrorResult("Error: " + e.getMessage());
        }
    }

    /**
     * Scan project directory and collect all matching files.
     * Uses Files.walkFileTree for efficient recursive traversal.
     *
     * @param projectPath - Root directory to scan
     * @param excludePatterns - Patterns to exclude (e.g., "*.class", "node_modules/**")
     * @param includeExtensions - Extensions to include (e.g., ".java", ".py")
     * @return List of matching file paths
     */
    private List<Path> scanProjectFiles(Path projectPath,
                                        Set<String> excludePatterns,
                                        Set<String> includeExtensions) throws IOException {

        List<Path> files = new ArrayList<>();

        Files.walkFileTree(projectPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Check if directory should be excluded
                String relativePath = projectPath.relativize(dir).toString();

                if (shouldExclude(relativePath, excludePatterns)) {
                    log.debug("Excluding directory: {}", relativePath);
                    return FileVisitResult.SKIP_SUBTREE; // Don't traverse this directory
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String relativePath = projectPath.relativize(file).toString();
                String extension = "." + FilenameUtils.getExtension(file.getFileName().toString());

                // Check if file should be included
                if (!shouldExclude(relativePath, excludePatterns) &&
                        includeExtensions.contains(extension)) {
                    files.add(file);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    /**
     * Check if a path should be excluded based on patterns.
     * Supports wildcards: *, **, ?
     *
     * Examples:
     * - "*.class" matches any .class file
     * - "node_modules/**" matches anything under node_modules
     * - "temp?.txt" matches temp1.txt, tempA.txt, etc.
     *
     * @param path - Path to check
     * @param patterns - Exclude patterns
     * @return true if path should be excluded
     */
    private boolean shouldExclude(String path, Set<String> patterns) {
        PathMatcher matcher;

        for (String pattern : patterns) {
            try {
                // Convert pattern to glob syntax
                String globPattern = "glob:" + pattern;
                matcher = FileSystems.getDefault().getPathMatcher(globPattern);

                if (matcher.matches(Paths.get(path))) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("Invalid exclude pattern: {}", pattern);
            }
        }

        return false;
    }

    /**
     * Group files by their top-level folder.
     *
     * Example:
     * src/main/java/App.java → "src"
     * src/test/java/Test.java → "src"
     * config/application.yml → "config"
     * README.md → "root" (files in project root)
     *
     * @param projectPath - Project root path
     * @param files - List of files to group
     * @return Map of folder name → list of files
     */
    private Map<String, List<Path>> groupFilesByTopLevelFolder(Path projectPath,
                                                               List<Path> files) {
        Map<String, List<Path>> groups = new HashMap<>();

        for (Path file : files) {
            Path relativePath = projectPath.relativize(file);

            String topLevelFolder;
            if (relativePath.getNameCount() == 1) {
                // File is in project root
                topLevelFolder = "root";
            } else {
                // Get first directory in path
                topLevelFolder = relativePath.getName(0).toString();
            }

            groups.computeIfAbsent(topLevelFolder, k -> new ArrayList<>()).add(file);
        }

        return groups;
    }

    /**
     * Concatenate files with automatic splitting when size limit is exceeded.
     *
     * Output format:
     * === path/to/file.java ===
     * [file content]
     *
     * === path/to/another.java ===
     * [file content]
     *
     * If files exceed maxFileSizeMb, creates multiple files:
     * foldername-1.txt, foldername-2.txt, etc.
     *
     * @param files - Files to concatenate
     * @param projectPath - Project root (for relative paths)
     * @param outputPath - Where to save output files
     * @param folderName - Name for output file
     * @param maxFileSizeMb - Maximum size before splitting
     * @return List of generated output file paths
     */
    private List<String> concatenateFilesWithSplit(List<Path> files,
                                                   Path projectPath,
                                                   Path outputPath,
                                                   String folderName,
                                                   int maxFileSizeMb) throws IOException {

        List<String> outputFiles = new ArrayList<>();
        long maxSizeBytes = maxFileSizeMb * MB_TO_BYTES;

        int fileIndex = 1;
        long currentSize = 0;
        BufferedWriter writer = null;
        Path currentOutputFile = null;

        try {
            for (Path file : files) {
                String relativePath = projectPath.relativize(file).toString();
                String fileContent = Files.readString(file);
                String separator = String.format(FILE_SEPARATOR, relativePath);

                long entrySize = separator.getBytes().length + fileContent.getBytes().length + 2; // +2 for newlines

                // Check if we need to create a new output file
                if (writer == null || (currentSize + entrySize > maxSizeBytes)) {
                    // Close previous writer
                    if (writer != null) {
                        writer.close();
                    }

                    // Create new output file
                    currentOutputFile = outputPath.resolve(folderName + "-" + fileIndex + ".txt");
                    writer = Files.newBufferedWriter(currentOutputFile, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                    outputFiles.add(currentOutputFile.toString());

                    log.info("Created output file: {}", currentOutputFile.getFileName());

                    fileIndex++;
                    currentSize = 0;
                }

                // Write file separator and content
                writer.write(separator);
                writer.write(fileContent);
                writer.write("\n\n");

                currentSize += entrySize;
            }

        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        return outputFiles;
    }

    /**
     * Generate PROJECT_STRUCTURE.json showing complete project tree.
     *
     * Format:
     * {
     *   "projectPath": "/path/to/project",
     *   "structure": {
     *     "src": {
     *       "main": {
     *         "java": ["App.java", "Controller.java"]
     *       }
     *     },
     *     "README.md": null
     *   }
     * }
     *
     * @param projectPath - Project root
     * @param files - All files in project
     * @param outputPath - Where to save structure file
     * @return Path to structure file
     */
    private String generateProjectStructure(Path projectPath,
                                            List<Path> files,
                                            Path outputPath) throws IOException {

        Map<String, Object> structure = new HashMap<>();
        structure.put("projectPath", projectPath.toString());

        Map<String, Object> tree = new HashMap<>();

        for (Path file : files) {
            Path relativePath = projectPath.relativize(file);
            addToTree(tree, relativePath);
        }

        structure.put("structure", tree);

        Path structureFile = outputPath.resolve(STRUCTURE_FILE_NAME);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(structureFile.toFile(), structure);

        log.info("Generated project structure: {}", structureFile);

        return structureFile.toString();
    }

    /**
     * Helper method to build tree structure recursively.
     */
    private void addToTree(Map<String, Object> tree, Path path) {
        if (path.getNameCount() == 1) {
            // Leaf node (file)
            tree.put(path.toString(), null);
        } else {
            // Directory node
            String firstPart = path.getName(0).toString();
            Path remaining = path.subpath(1, path.getNameCount());

            @SuppressWarnings("unchecked")
            Map<String, Object> subtree = (Map<String, Object>) tree.computeIfAbsent(
                    firstPart,
                    k -> new HashMap<String, Object>()
            );

            addToTree(subtree, remaining);
        }
    }

    /**
     * Load metadata from previous scan.
     * Returns null if no metadata exists.
     */
    private ProjectMetadata loadMetadata(Path projectPath) {
        Path metadataPath = projectPath.resolve(METADATA_FILE_NAME);

        if (!Files.exists(metadataPath)) {
            return null;
        }

        try {
            return objectMapper.readValue(metadataPath.toFile(), ProjectMetadata.class);
        } catch (IOException e) {
            log.warn("Failed to load metadata, will perform full scan", e);
            return null;
        }
    }

    /**
     * Save metadata for future incremental updates.
     */
    private String saveMetadata(Path projectPath, ProjectMetadata metadata) throws IOException {
        Path metadataPath = projectPath.resolve(METADATA_FILE_NAME);

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(metadataPath.toFile(), metadata);

        log.info("Saved metadata: {}", metadataPath);

        return metadataPath.toString();
    }

    /**
     * Merge request settings with user settings.
     * Request settings take priority.
     */
    private Set<String> mergeSettings(Set<String> requestSettings, Set<String> userSettings) {
        if (requestSettings != null && !requestSettings.isEmpty()) {
            return requestSettings;
        }
        return userSettings != null ? userSettings : new HashSet<>();
    }

    /**
     * Create error result.
     */
    private ConcatenationResult createErrorResult(String message) {
        return ConcatenationResult.builder()
                .success(false)
                .message(message)
                .errors(Collections.singletonList(message))
                .build();
    }
}
