package com.mranalizer.adapter.in.rest;

import com.mranalizer.adapter.in.rest.dto.AnalysisResponse;
import com.mranalizer.adapter.in.rest.dto.MrDetailResponse;
import com.mranalizer.application.dto.AnalysisRequestDto;
import com.mranalizer.application.dto.AnalysisSummaryDto;
import com.mranalizer.domain.exception.ReportNotFoundException;
import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.AnalysisResult;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.port.in.AnalyzeMrUseCase;
import com.mranalizer.domain.port.in.GetAnalysisResultsUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AnalysisRestController {

    private final AnalyzeMrUseCase analyzeMrUseCase;
    private final GetAnalysisResultsUseCase getAnalysisResultsUseCase;

    public AnalysisRestController(AnalyzeMrUseCase analyzeMrUseCase,
                                  GetAnalysisResultsUseCase getAnalysisResultsUseCase) {
        this.analyzeMrUseCase = analyzeMrUseCase;
        this.getAnalysisResultsUseCase = getAnalysisResultsUseCase;
    }

    @PostMapping("/analysis")
    public ResponseEntity<AnalysisResponse> triggerAnalysis(@Valid @RequestBody AnalysisRequestDto request) {
        FetchCriteria criteria = request.toFetchCriteria();
        AnalysisReport report = analyzeMrUseCase.analyze(criteria, request.useLlm(), request.selectedMrIds());
        return ResponseEntity.ok(AnalysisResponse.from(report));
    }

    @GetMapping("/analysis")
    public ResponseEntity<List<AnalysisResponse>> listReports(
            @RequestParam(required = false) String projectSlug) {
        List<AnalysisReport> reports;
        if (projectSlug != null && !projectSlug.isBlank()) {
            reports = getAnalysisResultsUseCase.getReportsByProjectSlug(projectSlug);
        } else {
            reports = getAnalysisResultsUseCase.getAllReports();
        }

        List<AnalysisResponse> response = reports.stream()
                .map(AnalysisResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analysis/{reportId}")
    public ResponseEntity<AnalysisResponse> getReport(@PathVariable Long reportId) {
        AnalysisReport report = getAnalysisResultsUseCase.getReport(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + reportId));
        return ResponseEntity.ok(AnalysisResponse.from(report));
    }

    @DeleteMapping("/analysis/{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long reportId) {
        analyzeMrUseCase.deleteAnalysis(reportId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analysis/{reportId}/mrs/{resultId}")
    public ResponseEntity<MrDetailResponse> getMrDetail(@PathVariable Long reportId,
                                                         @PathVariable Long resultId) {
        AnalysisResult result = getAnalysisResultsUseCase.getResult(reportId, resultId)
                .orElseThrow(() -> new ReportNotFoundException(
                        "Result not found: reportId=" + reportId + ", resultId=" + resultId));
        return ResponseEntity.ok(MrDetailResponse.from(result));
    }

    @GetMapping("/summary/{reportId}")
    public ResponseEntity<AnalysisSummaryDto> getSummary(@PathVariable Long reportId) {
        AnalysisReport report = getAnalysisResultsUseCase.getReport(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + reportId));

        int total = report.getTotalMrs();
        double autoPerc = total > 0 ? (double) report.getAutomatableCount() / total * 100 : 0;
        double maybePerc = total > 0 ? (double) report.getMaybeCount() / total * 100 : 0;
        double notSuitPerc = total > 0 ? (double) report.getNotSuitableCount() / total * 100 : 0;

        AnalysisSummaryDto summary = new AnalysisSummaryDto(
                report.getId(),
                report.getProjectSlug(),
                total,
                new AnalysisSummaryDto.VerdictCount(report.getAutomatableCount(), autoPerc),
                new AnalysisSummaryDto.VerdictCount(report.getMaybeCount(), maybePerc),
                new AnalysisSummaryDto.VerdictCount(report.getNotSuitableCount(), notSuitPerc)
        );

        return ResponseEntity.ok(summary);
    }
}
