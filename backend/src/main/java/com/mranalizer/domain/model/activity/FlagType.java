package com.mranalizer.domain.model.activity;

public enum FlagType {
    LARGE_PR("Za duży PR"),
    VERY_LARGE_PR("Bardzo duży PR"),
    QUICK_REVIEW("Zbyt krótki review"),
    SUSPICIOUS_QUICK_MERGE("Podejrzanie szybki merge"),
    WEEKEND_WORK("Praca w weekend"),
    NIGHT_WORK("Praca nocna"),
    NO_REVIEW("Brak review"),
    SELF_MERGE("Self-merge"),
    HIGH_WEEKEND_RATIO("Wysoki odsetek pracy weekendowej");

    private final String displayName;

    FlagType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
