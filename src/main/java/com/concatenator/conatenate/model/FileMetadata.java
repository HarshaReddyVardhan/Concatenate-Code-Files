package com.concatenator.conatenate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    private String filePath;

    private String sha256Hash;

    private Long lastModified;

    private Long fileSize;
}
