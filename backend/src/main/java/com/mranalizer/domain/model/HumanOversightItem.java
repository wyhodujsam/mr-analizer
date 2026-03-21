package com.mranalizer.domain.model;

/**
 * An area of the PR that requires human oversight with explanation why.
 */
public record HumanOversightItem(
        String area,
        String reasoning
) {}
