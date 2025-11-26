package com.concatenator.conatenate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMetadata {

    private String projectPath;

    private Long lastScanTime;

    @Builder.Default
    private Map<String, FileMetadata> files = new HashMap<>();

    private Integer totalFiles;

    private Long totalSize;
}
