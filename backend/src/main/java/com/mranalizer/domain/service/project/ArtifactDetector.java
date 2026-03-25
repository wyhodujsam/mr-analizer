package com.mranalizer.domain.service.project;

import com.mranalizer.domain.model.ChangedFile;
import com.mranalizer.domain.model.project.DetectionPatterns;

import java.util.List;

public class ArtifactDetector {

    private final DetectionPatterns patterns;

    public ArtifactDetector(DetectionPatterns patterns) {
        this.patterns = patterns;
    }

    public boolean hasBdd(List<ChangedFile> files) {
        return files.stream().anyMatch(f -> matchesAny(f.path(), patterns.bddPatterns()));
    }

    public boolean hasSdd(List<ChangedFile> files) {
        return files.stream().anyMatch(f -> matchesAny(f.path(), patterns.sddPatterns()));
    }

    public List<String> findBddFiles(List<ChangedFile> files) {
        return files.stream()
                .map(ChangedFile::path)
                .filter(path -> matchesAny(path, patterns.bddPatterns()))
                .toList();
    }

    public List<String> findSddFiles(List<ChangedFile> files) {
        return files.stream()
                .map(ChangedFile::path)
                .filter(path -> matchesAny(path, patterns.sddPatterns()))
                .toList();
    }

    private boolean matchesAny(String path, List<String> patterns) {
        if (path == null || patterns == null) return false;
        String normalizedPath = path.replace('\\', '/');
        String fileName = normalizedPath.contains("/")
                ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1)
                : normalizedPath;

        for (String pattern : patterns) {
            if (matchesGlob(normalizedPath, fileName, pattern)) return true;
        }
        return false;
    }

    private boolean matchesGlob(String fullPath, String fileName, String pattern) {
        if (pattern.startsWith("*")) {
            // e.g. "*.feature" → fileName ends with ".feature"
            // e.g. "*Steps.java" → fileName ends with "Steps.java"
            String suffix = pattern.substring(1);
            return fileName.endsWith(suffix);
        }
        // e.g. "spec.md" → fileName equals "spec.md"
        return fileName.equals(pattern);
    }
}
