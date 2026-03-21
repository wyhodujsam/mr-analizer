package com.mranalizer.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.domain.exception.ReportNotFoundException;
import com.mranalizer.domain.model.*;
import com.mranalizer.domain.port.in.AnalyzeMrUseCase;
import com.mranalizer.domain.port.in.GetAnalysisResultsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalysisRestController.class)
class AnalysisRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyzeMrUseCase analyzeMrUseCase;

    @MockBean
    private GetAnalysisResultsUseCase getAnalysisResultsUseCase;

    private AnalysisReport sampleReport() {
        MergeRequest mr = MergeRequest.builder()
                .externalId("100")
                .title("Test MR")
                .author("dev")
                .url("https://github.com/test/repo/pull/100")
                .build();

        AnalysisResult result = AnalysisResult.builder()
                .id(1L)
                .mergeRequest(mr)
                .score(0.8)
                .verdict(Verdict.AUTOMATABLE)
                .reasons(List.of("Small diff"))
                .matchedRules(List.of("small-diff-boost"))
                .llmComment("Looks good")
                .build();

        return AnalysisReport.of(1L, "owner/repo", "github",
                LocalDateTime.of(2026, 3, 21, 10, 0), List.of(result));
    }

    @Test
    void triggerAnalysis_returnsOk() throws Exception {
        AnalysisReport report = sampleReport();
        when(analyzeMrUseCase.analyze(any(), anyBoolean(), anyList())).thenReturn(report);

        String requestBody = """
                {
                    "projectSlug": "owner/repo",
                    "provider": "github",
                    "state": "merged",
                    "limit": 50,
                    "useLlm": false,
                    "selectedMrIds": []
                }
                """;

        mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(1))
                .andExpect(jsonPath("$.projectSlug").value("owner/repo"))
                .andExpect(jsonPath("$.totalMrs").value(1))
                .andExpect(jsonPath("$.results[0].title").value("Test MR"));
    }

    @Test
    void listReports_returnsAll() throws Exception {
        when(getAnalysisResultsUseCase.getAllReports()).thenReturn(List.of(sampleReport()));

        mockMvc.perform(get("/api/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportId").value(1))
                .andExpect(jsonPath("$[0].projectSlug").value("owner/repo"));
    }

    @Test
    void listReports_filtersByProjectSlug() throws Exception {
        when(getAnalysisResultsUseCase.getReportsByProjectSlug("owner/repo"))
                .thenReturn(List.of(sampleReport()));

        mockMvc.perform(get("/api/analysis").param("projectSlug", "owner/repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectSlug").value("owner/repo"));

        verify(getAnalysisResultsUseCase).getReportsByProjectSlug("owner/repo");
        verify(getAnalysisResultsUseCase, never()).getAllReports();
    }

    @Test
    void getReport_returnsOk() throws Exception {
        when(getAnalysisResultsUseCase.getReport(1L)).thenReturn(Optional.of(sampleReport()));

        mockMvc.perform(get("/api/analysis/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(1));
    }

    @Test
    void getReport_notFound_returns404() throws Exception {
        when(getAnalysisResultsUseCase.getReport(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/analysis/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void deleteReport_returnsNoContent() throws Exception {
        doNothing().when(analyzeMrUseCase).deleteAnalysis(1L);

        mockMvc.perform(delete("/api/analysis/1"))
                .andExpect(status().isNoContent());

        verify(analyzeMrUseCase).deleteAnalysis(1L);
    }

    @Test
    void getMrDetail_returnsOk() throws Exception {
        MergeRequest mr = MergeRequest.builder()
                .externalId("100")
                .title("Test MR")
                .author("dev")
                .sourceBranch("feature/x")
                .targetBranch("main")
                .state("merged")
                .url("https://github.com/test/repo/pull/100")
                .diffStats(new DiffStats(20, 5, 3))
                .build();

        AnalysisResult result = AnalysisResult.builder()
                .id(10L)
                .mergeRequest(mr)
                .score(0.7)
                .verdict(Verdict.AUTOMATABLE)
                .reasons(List.of("Clean code"))
                .matchedRules(List.of("test-boost"))
                .llmComment("Good")
                .build();

        when(getAnalysisResultsUseCase.getResult(1L, 10L)).thenReturn(Optional.of(result));

        mockMvc.perform(get("/api/analysis/1/mrs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultId").value(10))
                .andExpect(jsonPath("$.title").value("Test MR"))
                .andExpect(jsonPath("$.verdict").value("AUTOMATABLE"))
                .andExpect(jsonPath("$.additions").value(20));
    }

    @Test
    void getMrDetail_notFound_returns404() throws Exception {
        when(getAnalysisResultsUseCase.getResult(1L, 999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/analysis/1/mrs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSummary_returnsOk() throws Exception {
        when(getAnalysisResultsUseCase.getReport(1L)).thenReturn(Optional.of(sampleReport()));

        mockMvc.perform(get("/api/summary/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(1))
                .andExpect(jsonPath("$.totalMrs").value(1))
                .andExpect(jsonPath("$.automatable.count").value(1))
                .andExpect(jsonPath("$.automatable.percentage").value(100.0));
    }

    @Test
    void getSummary_notFound_returns404() throws Exception {
        when(getAnalysisResultsUseCase.getReport(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/summary/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void triggerAnalysis_missingProjectSlug_returns400() throws Exception {
        String requestBody = """
                {
                    "projectSlug": "",
                    "provider": "github"
                }
                """;

        mockMvc.perform(post("/api/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
