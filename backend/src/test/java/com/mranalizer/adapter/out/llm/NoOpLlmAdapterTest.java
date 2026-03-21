package com.mranalizer.adapter.out.llm;

import com.mranalizer.domain.model.LlmAssessment;
import com.mranalizer.domain.model.MergeRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoOpLlmAdapterTest {

    private final NoOpLlmAdapter adapter = new NoOpLlmAdapter();

    @Test
    void analyze_returnsZeroAssessment() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("1")
                .title("test")
                .build();

        LlmAssessment result = adapter.analyze(mr);

        assertEquals(0.0, result.scoreAdjustment(), 0.01);
        assertNull(result.comment());
        assertEquals("none", result.provider());
    }

    @Test
    void getProviderName_returnsNone() {
        assertEquals("none", adapter.getProviderName());
    }

    @Test
    void analyze_returnsEmptyDetailedFields() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("2")
                .title("another test")
                .build();

        LlmAssessment result = adapter.analyze(mr);

        assertEquals(0, result.overallAutomatability());
        assertTrue(result.categories().isEmpty());
        assertTrue(result.humanOversightRequired().isEmpty());
        assertTrue(result.whyLlmFriendly().isEmpty());
        assertTrue(result.summaryTable().isEmpty());
    }
}
