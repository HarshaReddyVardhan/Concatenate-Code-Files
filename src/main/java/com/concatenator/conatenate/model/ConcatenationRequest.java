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
}
