package com.mranalizer.domain.model.activity;

public record ActivityFlag(
        FlagType type,
        Severity severity,
        String description,
        String prReference
) {
}
