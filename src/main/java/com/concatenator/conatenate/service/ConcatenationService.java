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
import java.nio.file.attribute.DosFileAttributeView;
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
    private static final String XML_FILE_START = "<file path=\"%s\">\n";
    private static final String XML_FILE_END = "</file>\n";
    private static final long MB_TO_BYTES = 1024 * 1024;

    // Config files to ALWAYS include (useful for AI context)
    private static final Set<String> ALWAYS_INCLUDE_FILES = Set.of(
            "Dockerfile", "Jenkinsfile", "Makefile", "Procfile",
            "docker-compose.yml", "docker-compose.yaml",
            "pom.xml", "build.gradle", "settings.gradle",
            "Cargo.toml", "Cargo.lock",               // Rust
            "go.mod", "go.sum",                       // Go
            "package.json", "yarn.lock", "pnpm-lock.yaml", // Node/JS
            "composer.json", "composer.lock",         // PHP
            "requirements.txt", "pyproject.toml",     // Python
            "mix.exs",                                // Elixir
            ".gitignore", ".dockerignore");


    // Config/script extensions to ALWAYS include
    private static final Set<String> ALWAYS_INCLUDE_EXTENSIONS = Set.of(
            ".yml", ".yaml", ".xml", ".json", ".properties", ".conf",
            ".sh", ".bat", ".cmd", ".ps1",
            ".toml", ".ini", ".cfg", ".env",
            ".hcl", ".tf",          // Terraform / HashiCorp
            ".proto",               // Protocol Buffers
            ".graphql", ".gql",     // GraphQL
            ".sql");                // Database migrations


    // Directories to ALWAYS exclude (build artifacts, dependencies)
    private static final Set<String> DEFAULT_EXCLUDE_PATTERNS = Set.of(
            "target/**", "build/**", "dist/**", "out/**",
            "node_modules/**", ".git/**", "bin/**", "obj/**",
            "vendor/**",            // Go dependencies / PHP
            "testdata/**",          // Go test data
            "__pycache__/**", ".pytest_cache/**", "venv/**", ".venv/**",
            ".idea/**", ".vscode/**", ".zed/**", // Editors
            "*.exe", "*.dll", "*.so", "*.dylib", "*.class", "*.jar", "*.war",
            "*.pdb", "*.out", "*.test"); // Build outputs and debug symbols


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
                    userSettings.getExcludePatterns());
            Set<String> includeExtensions = mergeSettings(
                    request.getIncludeExtensions(),
                    userSettings.getIncludeExtensions());

            String outputFolderRequest = request.getOutputFolder();
            String defaultFolderName = projectPath.getFileName().toString() + "_Concatenated_Output";
            Path outputPath;

            if (outputFolderRequest == null || outputFolderRequest.trim().isEmpty()) {
                // Default: create the output folder inside the project directory
                outputPath = projectPath.resolve(defaultFolderName);
            } else {
                Path providedPath = Paths.get(outputFolderRequest);
                if (providedPath.isAbsolute()) {
                    // Absolute path: create the subfolder inside the specified directory
                    outputPath = providedPath.resolve(defaultFolderName);
                } else {
                    // Relative path: resolve relative to project, then create subfolder inside
                    outputPath = projectPath.resolve(outputFolderRequest).resolve(defaultFolderName);
                }
            }

            int maxFileSizeMb = request.getMaxFileSizeMb() != null
                    ? request.getMaxFileSizeMb()
                    : userSettings.getDefaultMaxFileSizeMb();

            Files.createDirectories(outputPath);

            // Step 4: Load previous metadata (for incremental updates)
            // now
            ProjectMetadata previousMetadata = loadMetadata(outputPath);
            boolean isIncremental = Boolean.TRUE.equals(request.getIncrementalUpdate())
                    && previousMetadata != null;

            // FIX: Add output folder to exclude patterns PRE-SCAN
            if (outputPath.startsWith(projectPath)) {
                String relativeOut = projectPath.relativize(outputPath).toString();
                excludePatterns.add(relativeOut);
                excludePatterns.add(relativeOut + "/**");
            }
            // Add default exclusions for build artifacts and dependencies
            excludePatterns.addAll(DEFAULT_EXCLUDE_PATTERNS);
            // Exclude the metadata file itself
            excludePatterns.add(METADATA_FILE_NAME);

            // Step 4: Scan project files
            log.info("Scanning project files...");
            List<Path> allFiles = scanProjectFiles(projectPath, excludePatterns, includeExtensions);

            // AUTO-FIX: Double check exclusion
            if (outputPath.startsWith(projectPath)) {
                allFiles = allFiles.stream()
                        .filter(file -> !file.startsWith(outputPath))
                        .collect(Collectors.toList());
            }

            // Filter by selected paths if provided (and not empty)
            if (request.getSelectedFilePaths() != null && !request.getSelectedFilePaths().isEmpty()) {
                Set<String> selectedSet = new HashSet<>(request.getSelectedFilePaths());
                // Normalize selected paths to ensure matching works
                // The frontend should send paths relative to project root
                log.info("Filtering with {} selected files", selectedSet.size());

                allFiles = allFiles.stream()
                        .filter(file -> {
                            String relPath = projectPath.relativize(file).toString().replace("\\", "/");
                            // Also check standard separator just in case
                            String relPathStd = projectPath.relativize(file).toString();
                            return selectedSet.contains(relPath) || selectedSet.contains(relPathStd);
                        })
                        .collect(Collectors.toList());
            }

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
                    filesToProcess);

            // Step 7: Output directory already created in Step 3
            // Path outputPath = projectPath.resolve(outputFolder);
            // Files.createDirectories(outputPath);

            // Step 8: Generate concatenated files for each folder
            List<String> outputFiles = new ArrayList<>();
            long totalSize = 0;
            long totalTokens = 0;
            String asciiTree = null;

            if (Boolean.TRUE.equals(request.getIncludeFileTree())) {
                asciiTree = generateAsciiTree(projectPath, allFiles);
            }

            for (Map.Entry<String, List<Path>> entry : filesByFolder.entrySet()) {
                String folderName = entry.getKey();
                List<Path> files = entry.getValue();

                ConcatenationOutput output = concatenateFilesWithSplit(
                        files,
                        projectPath,
                        outputPath,
                        folderName,
                        maxFileSizeMb,
                        request.getUseXmlTags(),
                        request.getIncludeFileTree() ? asciiTree : null);

                outputFiles.addAll(output.getFilePaths());
                totalTokens += output.getTokenCount();
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

            // FIX: Save metadata to outputPath
            String metadataFile = saveMetadata(outputPath, newMetadata);

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
                    .estimatedTokenCount(Boolean.TRUE.equals(request.getEstimateTokens()) ? totalTokens : null)
                    .previewFileTree(asciiTree)
                    .processedFilePaths(filesToProcess.stream()
                            .map(path -> projectPath.relativize(path).toString())
                            .collect(Collectors.toList()))
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
     * @param projectPath       - Root directory to scan
     * @param excludePatterns   - Patterns to exclude (e.g., "*.class",
     *                          "node_modules/**")
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
                String fileName = file.getFileName().toString();
                String extension = "." + FilenameUtils.getExtension(fileName);

                // Skip if excluded
                if (shouldExclude(relativePath, excludePatterns)) {
                    return FileVisitResult.CONTINUE;
                }

                // Include if: matches user extensions OR is a config file/script
                boolean matchesUserExtension = includeExtensions.contains(extension);
                boolean isAlwaysIncludeFile = ALWAYS_INCLUDE_FILES.contains(fileName);
                boolean isAlwaysIncludeExtension = ALWAYS_INCLUDE_EXTENSIONS.contains(extension);

                if (matchesUserExtension || isAlwaysIncludeFile || isAlwaysIncludeExtension) {
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
     * @param path     - Path to check
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
     * @param files       - List of files to group
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
     * @param files         - Files to concatenate
     * @param projectPath   - Project root (for relative paths)
     * @param outputPath    - Where to save output files
     * @param folderName    - Name for output file
     * @param maxFileSizeMb - Maximum size before splitting
     * @return List of generated output file paths
     */
    private ConcatenationOutput concatenateFilesWithSplit(List<Path> files,
            Path projectPath,
            Path outputPath,
            String folderName,
            int maxFileSizeMb,
            Boolean useXmlTags,
            String asciiTree) throws IOException {

        List<String> outputFiles = new ArrayList<>();
        long maxSizeBytes = maxFileSizeMb * MB_TO_BYTES;

        int fileIndex = 1;
        long currentSize = 0;
        long tokenCount = 0;
        BufferedWriter writer = null;
        Path currentOutputFile = null;

        try {
            // Write Tree to first file if present
            if (asciiTree != null) {
                currentOutputFile = outputPath.resolve(folderName + "-" + fileIndex + ".txt");
                writer = Files.newBufferedWriter(currentOutputFile, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                outputFiles.add(currentOutputFile.toString());

                writer.write("PROJECT FILE TREE:\n");
                writer.write(asciiTree);
                writer.write("\n\n");
                currentSize += asciiTree.length();
                if (asciiTree != null)
                    tokenCount += asciiTree.length() / 4;
            }

            for (Path file : files) {
                String relativePath = projectPath.relativize(file).toString();
                String fileContent = Files.readString(file);

                String entryStart;
                String entryEnd = "\n\n";
                if (Boolean.TRUE.equals(useXmlTags)) {
                    entryStart = String.format(XML_FILE_START, relativePath);
                    entryEnd = XML_FILE_END + "\n";
                } else {
                    entryStart = String.format(FILE_SEPARATOR, relativePath);
                }

                long entrySize = entryStart.getBytes().length + fileContent.getBytes().length
                        + entryEnd.getBytes().length;

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

                // Write content
                writer.write(entryStart);
                writer.write(fileContent);
                writer.write(entryEnd);

                currentSize += entrySize;
                tokenCount += (entrySize / 4); // Rough approx
            }

        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        return new ConcatenationOutput(outputFiles, tokenCount);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ConcatenationOutput {
        private List<String> filePaths;
        private long tokenCount;
    }

    private String generateAsciiTree(Path projectPath, List<Path> files) {
        // Simple implementation for now - can be improved
        StringBuilder sb = new StringBuilder();
        // Sort files to ensure folders come together
        files.stream().sorted().forEach(path -> {
            String rel = projectPath.relativize(path).toString();
            int depth = path.getNameCount() - projectPath.getNameCount();
            // Basic indentation
            sb.append("  ".repeat(Math.max(0, depth)));
            sb.append("- ").append(path.getFileName()).append("\n");
        });
        return sb.toString();
    }

    /**
     * Generate PROJECT_STRUCTURE.json showing complete project tree.
     *
     * Format:
     * {
     * "projectPath": "/path/to/project",
     * "structure": {
     * "src": {
     * "main": {
     * "java": ["App.java", "Controller.java"]
     * }
     * },
     * "README.md": null
     * }
     * }
     *
     * @param projectPath - Project root
     * @param files       - All files in project
     * @param outputPath  - Where to save structure file
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
                    k -> new HashMap<String, Object>());

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
     * On Windows, the file is marked as hidden.
     */
    private String saveMetadata(Path projectPath, ProjectMetadata metadata) throws IOException {
        Path metadataPath = projectPath.resolve(METADATA_FILE_NAME);

        // If file exists and is hidden, unhide it before writing (otherwise Access
        // Denied on Windows)
        if (Files.exists(metadataPath)) {
            try {
                DosFileAttributeView dosView = Files.getFileAttributeView(metadataPath, DosFileAttributeView.class);
                if (dosView != null && dosView.readAttributes().isHidden()) {
                    dosView.setHidden(false);
                }
            } catch (Exception e) {
                log.warn("Failed to unhide metadata file before writing: {}", e.getMessage());
            }
        }

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(metadataPath.toFile(), metadata);

        // Make the file hidden on Windows
        try {
            DosFileAttributeView dosView = Files.getFileAttributeView(metadataPath, DosFileAttributeView.class);
            if (dosView != null) {
                dosView.setHidden(true);
                log.debug("Set hidden attribute on metadata file: {}", metadataPath);
            }
        } catch (Exception e) {
            log.warn("Could not set hidden attribute on metadata file (non-Windows OS?): {}", e.getMessage());
        }

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

    /**
     * Get the file tree structure for the project.
     * Used by the frontend for file selection.
     */
    public List<Map<String, Object>> getProjectFileTree(String projectPathStr) throws IOException {
        Path projectPath = Paths.get(projectPathStr);
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return Collections.emptyList();
        }

        return buildFileTreeNodes(projectPath, projectPath);
    }

    private List<Map<String, Object>> buildFileTreeNodes(Path root, Path currentDir) throws IOException {
        List<Map<String, Object>> nodes = new ArrayList<>();

        // Use try-with-resources to ensure the stream is closed
        try (var stream = Files.list(currentDir)) {
            List<Path> files = stream.collect(Collectors.toList());

            // Sort: Directories first, then files, both alphabetically
            files.sort((p1, p2) -> {
                boolean d1 = Files.isDirectory(p1);
                boolean d2 = Files.isDirectory(p2);
                if (d1 != d2) {
                    return d1 ? -1 : 1;
                }
                return p1.getFileName().toString().compareToIgnoreCase(p2.getFileName().toString());
            });

            for (Path path : files) {
                String fileName = path.getFileName().toString();

                // Skip .git folder
                if (Files.isDirectory(path) && ".git".equals(fileName)) {
                    continue;
                }

                Map<String, Object> node = new HashMap<>();
                node.put("name", fileName);
                node.put("path", root.relativize(path).toString().replace("\\", "/"));

                if (Files.isDirectory(path)) {
                    node.put("type", "directory");
                    node.put("children", buildFileTreeNodes(root, path));
                } else {
                    node.put("type", "file");
                }

                nodes.add(node);
            }
        } catch (AccessDeniedException e) {
            log.warn("Access denied reading directory: {}", currentDir);
            // Return empty list or partially filled list for this directory
        }

        return nodes;
    }
}
