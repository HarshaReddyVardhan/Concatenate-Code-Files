package com.concatenator.conatenate.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcatenationRequest {

    @NotBlank(message = "Project path is required")
    private String projectPath;

    private String outputFolder;

    private Integer maxFileSizeMb;

    private Set<String> excludePatterns;

    private Set<String> includeExtensions;

    private Boolean incrementalUpdate;

    // Optional: exact list of files to include (for Tree View selection)
    private java.util.List<String> selectedFilePaths;

    // AI Agent Features
    @Builder.Default
    private Boolean useXmlTags = false;

    @Builder.Default
    private Boolean includeFileTree = false;

    @Builder.Default
    private Boolean estimateTokens = false;

    @Builder.Default
    private Boolean removeComments = false;

    @Builder.Default
    private Boolean removeRedundantWhitespace = false;

    @Builder.Default
    private Boolean includeFileHeader = true;
}
