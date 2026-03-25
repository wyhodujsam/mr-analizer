package com.mranalizer.domain.model.project;

import java.util.List;

public record DetectionPatterns(
        List<String> bddPatterns,
        List<String> sddPatterns
) {}
