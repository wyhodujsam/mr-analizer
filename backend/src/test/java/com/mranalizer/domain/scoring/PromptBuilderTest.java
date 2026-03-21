package com.mranalizer.domain.scoring;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.MergeRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    private MergeRequest.Builder baseMrBuilder() {
        return MergeRequest.builder()
                .title("Fix login bug")
                .description("Fixes null pointer on login page")
                .author("john.doe")
                .sourceBranch("feature/login-fix")
                .targetBranch("main")
                .labels(List.of("bugfix", "frontend"))
                .diffStats(new DiffStats(10, 5, 3))
                .hasTests(true);
    }

    @Test
    void build_replacesAllPlaceholders() {
        String template = "T:{{title}} D:{{description}} F:{{filesChanged}} A:{{additions}} " +
                "DEL:{{deletions}} HT:{{hasTests}} L:{{labels}} AU:{{author}} " +
                "SB:{{sourceBranch}} TB:{{targetBranch}}";

        MergeRequest mr = baseMrBuilder().build();
        String result = promptBuilder.build(template, mr);

        assertEquals("T:Fix login bug D:Fixes null pointer on login page F:3 A:10 " +
                "DEL:5 HT:true L:bugfix, frontend AU:john.doe " +
                "SB:feature/login-fix TB:main", result);
    }

    @Test
    void build_nullDescription_replacesWithNone() {
        String template = "Desc: {{description}}";
        MergeRequest mr = baseMrBuilder().description(null).build();

        String result = promptBuilder.build(template, mr);

        assertEquals("Desc: none", result);
    }

    @Test
    void build_emptyLabels_replacesWithNone() {
        String template = "Labels: {{labels}}";
        MergeRequest mr = baseMrBuilder().labels(Collections.emptyList()).build();

        String result = promptBuilder.build(template, mr);

        assertEquals("Labels: none", result);
    }

    @Test
    void build_nullTemplate_usesDefault() {
        MergeRequest mr = baseMrBuilder().build();

        String result = promptBuilder.build(null, mr);

        assertTrue(result.contains("Fix login bug"));
        assertTrue(result.contains("Przeanalizuj tego Pull Requesta"));
        assertTrue(result.contains("Branch zrodlowy: feature/login-fix"));
        assertTrue(result.contains("Branch docelowy: main"));
    }

    @Test
    void build_blankTemplate_usesDefault() {
        MergeRequest mr = baseMrBuilder().build();

        String result = promptBuilder.build("   ", mr);

        assertTrue(result.contains("Fix login bug"));
        assertTrue(result.contains("Przeanalizuj tego Pull Requesta"));
    }

    @Test
    void build_customTemplate_works() {
        String template = "Review: {{title}} by {{author}}";
        MergeRequest mr = baseMrBuilder().build();

        String result = promptBuilder.build(template, mr);

        assertEquals("Review: Fix login bug by john.doe", result);
    }

    @Test
    void build_unknownPlaceholder_replacedWithEmptyString() {
        String template = "Title: {{title}} Unknown: {{unknown}}";
        MergeRequest mr = baseMrBuilder().build();

        String result = promptBuilder.build(template, mr);

        assertEquals("Title: Fix login bug Unknown: ", result);
    }

    @Test
    void build_nullDiffStats_replacesWithZeros() {
        String template = "F:{{filesChanged}} A:{{additions}} D:{{deletions}}";
        MergeRequest mr = baseMrBuilder().diffStats(null).build();

        String result = promptBuilder.build(template, mr);

        assertEquals("F:0 A:0 D:0", result);
    }
}
