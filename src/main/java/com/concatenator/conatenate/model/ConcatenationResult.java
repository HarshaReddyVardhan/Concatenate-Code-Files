package com.concatenator.conatenate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcatenationResult {

    private Boolean success;

    private String message;

    @Builder.Default
    private List<String> outputFiles = new ArrayList<>();

    private Integer totalFilesProcessed;

    private Integer filesChanged;

    private Integer filesSkipped;

    private Long totalSizeBytes;

    private Long processingTimeMs;

    private String projectStructureFile;

    private String metadataFile;

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
