package com.concatenator.conatenate.service;

import com.concatenator.conatenate.model.ConcatenationRequest;
import com.concatenator.conatenate.model.ConcatenationResult;
import com.concatenator.conatenate.model.ProjectMetadata;
import com.concatenator.conatenate.model.UserSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConcatenationServiceTest {

    private ConcatenationService concatenationService;
    private FileHashService fileHashService;
    private UserSettingsService userSettingsService;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        fileHashService = mock(FileHashService.class);
        userSettingsService = mock(UserSettingsService.class);
        objectMapper = new ObjectMapper();

        when(userSettingsService.loadSettings()).thenReturn(UserSettings.builder()
                .defaultOutputFolder("concat-output")
                .defaultMaxFileSizeMb(30)
                .build());

        // Simple hash mock
        when(fileHashService.calculateFileHash(any())).thenAnswer(invocation -> {
            Path p = invocation.getArgument(0);
            return "hash-" + p.getFileName().toString() + "-" + Files.getLastModifiedTime(p).toMillis();
        });

        concatenationService = new ConcatenationService(fileHashService, userSettingsService, objectMapper);
    }

    @Test
    void testMetadataStoresInOutputFolder() throws IOException {
        // Setup simple project structure
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Main.java"), "public class Main {}");

        ConcatenationRequest request = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder("output-folder")
                .includeExtensions(new HashSet<>(Collections.singletonList(".java")))
                .build();

        // Run concatenation
        ConcatenationResult result = concatenationService.generateConcatenation(request);

        // Verify success
        assertTrue(result.getSuccess());

        // Check metadata file location
        Path outputDir = tempDir.resolve("output-folder");
        Path metadataFile = outputDir.resolve(".project-concat-metadata.json");
        assertTrue(Files.exists(metadataFile), "Metadata file should exist in output folder");

        // Check it does NOT exist in root
        assertFalse(Files.exists(tempDir.resolve(".project-concat-metadata.json")),
                "Metadata file should NOT exist in root");
    }

    @Test
    void testOutputFolderExclusion_BugFix() throws IOException {
        // Scenario: Output folder matches the default naming convention or custom name
        // and is inside the project root. We must ensure files in here are NOT
        // concatenated.

        String outputFolderName = "Project-Concatenated-Output";
        Path outputDir = tempDir.resolve(outputFolderName);
        Files.createDirectories(outputDir);

        // Create a "stale" output file in the output directory
        Files.writeString(outputDir.resolve("stale-output.txt"), "Old content from previous run");

        // Create a valid source file
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("RealCode.java"), "public class RealCode {}");

        ConcatenationRequest request = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder(outputFolderName)
                .includeExtensions(new HashSet<>(Arrays.asList(".java", ".txt"))) // Intentionally include .txt
                .build();

        ConcatenationResult result = concatenationService.generateConcatenation(request);

        assertTrue(result.getSuccess());
        assertEquals(1, result.getTotalFilesProcessed(), "Should only process 1 file (RealCode.java)");

        // Verify output matches expectation
        List<String> outputFiles = result.getOutputFiles();
        boolean staleFileIncluded = outputFiles.stream().anyMatch(f -> f.contains("stale-output"));
        assertFalse(staleFileIncluded, "Output file from output folder should NOT be included in concatenation");
    }

    @Test
    void testXmlTagsAndFileTree() throws IOException {
        // Setup
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Test.java"), "class Test {}");

        ConcatenationRequest request = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder("out")
                .includeExtensions(new HashSet<>(Collections.singletonList(".java")))
                .useXmlTags(true)
                .includeFileTree(true)
                .estimateTokens(true)
                .build();

        ConcatenationResult result = concatenationService.generateConcatenation(request);

        assertTrue(result.getSuccess());
        assertNotNull(result.getEstimatedTokenCount());
        assertNotNull(result.getPreviewFileTree());

        // Check content
        Path outputFile = tempDir.resolve("out").resolve("src-1.txt");
        String content = Files.readString(outputFile);

        assertTrue(content.contains("PROJECT FILE TREE:"), "Should contain file tree header");
        assertTrue(content.contains("<file path="), "Should contain XML tags");
        assertTrue(content.contains("</file>"), "Should contain closing XML tags");
    }

    @Test
    void testIncrementalUpdates() throws IOException, InterruptedException {
        // Step 1: Initial Run
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Path file1 = src.resolve("File1.java");
        Path file2 = src.resolve("File2.java");
        Files.writeString(file1, "Content 1");
        Files.writeString(file2, "Content 2");

        ConcatenationRequest request = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder("out-incremental")
                .includeExtensions(new HashSet<>(Collections.singletonList(".java")))
                .incrementalUpdate(false)
                .build();

        ConcatenationResult result1 = concatenationService.generateConcatenation(request);
        assertTrue(result1.getSuccess());
        assertEquals(2, result1.getTotalFilesProcessed());

        // Step 2: Modify one file and Run Incremental
        // Sleep specifically to ensure lastModified timestamp changes (OS granularity)
        Thread.sleep(100);
        Files.writeString(file1, "Content 1 Modified");

        // Re-mock hash service for new content (since we mocked it simplistically in
        // setup, the timestamp change handles it)

        ConcatenationRequest requestInc = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder("out-incremental")
                .includeExtensions(new HashSet<>(Collections.singletonList(".java")))
                .incrementalUpdate(true)
                .build();

        ConcatenationResult result2 = concatenationService.generateConcatenation(requestInc);

        assertTrue(result2.getSuccess());
        // Should process 1 (modified), skip 1
        assertEquals(1, result2.getFilesChanged(), "Should detect 1 changed file");
        assertEquals(1, result2.getFilesSkipped(), "Should skip 1 unchanged file");
    }

    @Test
    void testExcludePatterns() throws IOException {
        Path root = tempDir;

        // Good files
        Files.writeString(root.resolve("Good.java"), "code");

        // Bad folders
        Path nodeModules = root.resolve("node_modules");
        Files.createDirectories(nodeModules);
        Files.writeString(nodeModules.resolve("bad.js"), "bad");

        // Bad patterns
        Files.writeString(root.resolve("secret.env"), "secret");

        ConcatenationRequest request = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder("out")
                .includeExtensions(new HashSet<>(Arrays.asList(".java", ".js", ".env")))
                .excludePatterns(new HashSet<>(Arrays.asList("node_modules/**", "*.env")))
                .build();

        ConcatenationResult result = concatenationService.generateConcatenation(request);

        assertTrue(result.getSuccess());
        // Should only find Good.java
        assertEquals(1, result.getTotalFilesProcessed());
        assertTrue(result.getOutputFiles().stream().anyMatch(f -> {
            try {
                return Files.readString(Path.of(f)).contains("Good.java");
            } catch (IOException e) {
                return false;
            }
        }));
    }

    @Test
    void testFileSplittingLimit() throws Exception {
        // Reflection magic to lower the MB_TO_BYTES constant for testing
        // Note: Modifying static final fields is risky and JDK version dependent.
        // Instead of reflection which might fail on stricter JDKs, we'll generate
        // enough data
        // OR better, we will trust the logic since we can't easily change the static
        // final without tools like PowerMock.
        // However, since the user ASKED for comprehensive tests, let's try a safe
        // approach:
        // We will create a test that sets maxFileSizeMb to 1, and write 1.5MB of data.

        Path heavyDir = tempDir.resolve("heavy");
        Files.createDirectories(heavyDir);

        // Create a large string (approx 1.2 MB)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12000; i++) {
            sb.append(
                    "This is a line of 100 characters to fill up the space... 1234567890 1234567890 1234567890 12345");
        }
        String content = sb.toString();
        Files.writeString(heavyDir.resolve("BigFile.txt"), content);
        Files.writeString(heavyDir.resolve("SmallFile.txt"), "Tiny content");

        ConcatenationRequest request = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder("out-split")
                .includeExtensions(new HashSet<>(Collections.singletonList(".txt")))
                .maxFileSizeMb(1) // 1 MB limit
                .useXmlTags(false)
                .build();

        ConcatenationResult result = concatenationService.generateConcatenation(request);
        assertTrue(result.getSuccess());

        // We expect at least 2 output files because BigFile (1.2MB) + SmallFile > 1MB
        // Actually, logic splits when `currentSize + entrySize > maxSizeBytes`.
        // BigFile alone > 1MB, so it might take one file. SmallFile might go to next.

        long outputCount = Files.list(tempDir.resolve("out-split"))
                .filter(p -> p.toString().endsWith(".txt"))
                .count();

        assertTrue(outputCount >= 2, "Should have split into at least 2 files given the size limit");
    }
}
