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

        // Check metadata file location - now inside the _Concatenated_Output subfolder
        String projectName = tempDir.getFileName().toString();
        Path outputDir = tempDir.resolve("output-folder").resolve(projectName + "_Concatenated_Output");
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
        // Note: The stale file in outputFolderName won't be concatenated since the
        // actual output goes to
        // outputFolderName/ProjectName_Concatenated_Output/
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

        // Check content - now inside the _Concatenated_Output subfolder
        String projectName = tempDir.getFileName().toString();
        Path outputFile = tempDir.resolve("out").resolve(projectName + "_Concatenated_Output").resolve("src-1.txt");
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

        assertTrue(result2.getSuccess(), "Incremental update failed: " + result2.getMessage());
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

        String projectName = tempDir.getFileName().toString();
        Path actualOutputDir = tempDir.resolve("out-split").resolve(projectName + "_Concatenated_Output");
        long outputCount = Files.list(actualOutputDir)
                .filter(p -> p.toString().endsWith(".txt"))
                .count();

        assertTrue(outputCount >= 2, "Should have split into at least 2 files given the size limit");
    }

    @Test
    void testConfigFilesIncludedAndMetadataExcluded() throws IOException {
        // Setup project with config files
        Path root = tempDir;

        // Create source code
        Path src = root.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("App.java"), "public class App {}");

        // Create config files that should ALWAYS be included
        Files.writeString(root.resolve("Dockerfile"), "FROM openjdk:17");
        Files.writeString(root.resolve("docker-compose.yml"), "version: '3'");
        Path k8s = root.resolve("k8s");
        Files.createDirectories(k8s);
        Files.writeString(k8s.resolve("deployment.yaml"), "apiVersion: apps/v1");
        Files.writeString(root.resolve("build.sh"), "#!/bin/bash\nmvn clean install");
        Files.writeString(root.resolve("deploy.bat"), "@echo off\necho Deploying...");

        // Create build artifact folder that should be excluded
        Path target = root.resolve("target");
        Files.createDirectories(target);
        Files.writeString(target.resolve("app.jar"), "BINARY_CONTENT");

        // Create fake metadata file that should be excluded
        Files.writeString(root.resolve(".project-concat-metadata.json"), "{\"old\":\"metadata\"}");

        ConcatenationRequest request = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder("output")
                .includeExtensions(new HashSet<>(Collections.singletonList(".java")))
                .build();

        ConcatenationResult result = concatenationService.generateConcatenation(request);

        assertTrue(result.getSuccess());

        // Gather all output content - now inside the _Concatenated_Output subfolder
        String projectName = tempDir.getFileName().toString();
        Path outputDir = tempDir.resolve("output").resolve(projectName + "_Concatenated_Output");
        StringBuilder allContent = new StringBuilder();
        for (String file : result.getOutputFiles()) {
            allContent.append(Files.readString(Path.of(file)));
        }
        String content = allContent.toString();

        // Verify config files ARE included
        assertTrue(content.contains("FROM openjdk:17"), "Dockerfile should be included");
        assertTrue(content.contains("version: '3'"), "docker-compose.yml should be included");
        assertTrue(content.contains("apiVersion: apps/v1"), "Kubernetes YAML should be included");
        assertTrue(content.contains("#!/bin/bash"), "Shell script should be included");
        assertTrue(content.contains("@echo off"), "Batch script should be included");
        assertTrue(content.contains("public class App"), "Java source should be included");

        // Verify build artifacts are EXCLUDED
        assertFalse(content.contains("BINARY_CONTENT"), "Build artifacts (target/) should be excluded");

        // Verify old metadata is EXCLUDED from the output content
        assertFalse(content.contains("\"old\":\"metadata\""), "Metadata file should be excluded from output");
    }

    @Test
    void testAbsolutePathCreatesSubfolder() throws IOException {
        // Scenario: User selects an absolute path (e.g. C:\Users\User\Desktop)
        // We expect the service to create a subfolder "ProjectName_Concatenated_Output"
        // inside it.

        Path externalDir = tempDir.resolve("external-location");
        Files.createDirectories(externalDir);

        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Code.java"), "class Code {}");

        ConcatenationRequest request = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder(externalDir.toAbsolutePath().toString()) // Absolute path
                .includeExtensions(new HashSet<>(Collections.singletonList(".java")))
                .build();

        ConcatenationResult result = concatenationService.generateConcatenation(request);

        assertTrue(result.getSuccess());

        // Check that files were NOT created directly in externalDir
        long filesInRoot = Files.list(externalDir).filter(Files::isRegularFile).count();
        assertEquals(0, filesInRoot, "Should not create files directly in the absolute path root");

        // Check that subfolder was created
        // Name depends on tempDir name, so we check for folder ending with
        // _Concatenated_Output
        String expectedSuffix = "_Concatenated_Output";
        // Or strictly: tempDir_Concatenated_Output
        String folderName = tempDir.getFileName().toString() + "_Concatenated_Output";
        Path expectedSubfolder = externalDir.resolve(folderName);

        assertTrue(Files.exists(expectedSubfolder), "Should create subfolder " + folderName);
        assertTrue(Files.isDirectory(expectedSubfolder));

        // Check files are inside
        assertTrue(Files.list(expectedSubfolder).findAny().isPresent(), "Subfolder should contain output files");
    }

    @Test
    void testRelativePathCreatesSubfolder() throws IOException {
        // BUG FIX TEST: When user provides a relative path (e.g., "my-output"),
        // the service should create the _Concatenated_Output subfolder inside it.
        // Before fix: files were placed directly in the relative folder.
        // After fix: files are placed in
        // relative-folder/ProjectName_Concatenated_Output/

        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("App.java"), "class App {}");

        String relativeOutputFolder = "custom-output-folder";
        ConcatenationRequest request = ConcatenationRequest.builder()
                .projectPath(tempDir.toString())
                .outputFolder(relativeOutputFolder) // Relative path
                .includeExtensions(new HashSet<>(Collections.singletonList(".java")))
                .build();

        ConcatenationResult result = concatenationService.generateConcatenation(request);

        assertTrue(result.getSuccess());

        // Check that the _Concatenated_Output subfolder was created
        String projectName = tempDir.getFileName().toString();
        Path expectedSubfolder = tempDir.resolve(relativeOutputFolder).resolve(projectName + "_Concatenated_Output");

        assertTrue(Files.exists(expectedSubfolder),
                "Should create subfolder " + projectName + "_Concatenated_Output inside " + relativeOutputFolder);
        assertTrue(Files.isDirectory(expectedSubfolder));

        // Check files are inside the subfolder, not directly in the relative folder
        assertTrue(Files.list(expectedSubfolder).findAny().isPresent(),
                "Subfolder should contain output files");

        // Verify that files are NOT directly in the custom-output-folder root
        Path customOutputFolder = tempDir.resolve(relativeOutputFolder);
        long filesDirectlyInRoot = Files.list(customOutputFolder)
                .filter(Files::isRegularFile)
                .count();
        assertEquals(0, filesDirectlyInRoot,
                "Files should NOT be placed directly in the relative output folder root");
    }
}
