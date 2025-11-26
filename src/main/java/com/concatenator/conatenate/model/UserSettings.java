package com.concatenator.conatenate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Builder.Default
    private Set<String> excludePatterns = new HashSet<>();

    @Builder.Default
    private Set<String> includeExtensions = new HashSet<>();

    private String defaultOutputFolder;

    private Integer defaultMaxFileSizeMb;

    private Long lastUpdated;
}
