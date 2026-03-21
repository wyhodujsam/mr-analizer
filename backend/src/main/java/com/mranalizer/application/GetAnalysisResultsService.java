package com.mranalizer.application;

import com.mranalizer.domain.model.AnalysisReport;
import com.mranalizer.domain.model.AnalysisResult;
import com.mranalizer.domain.port.in.GetAnalysisResultsUseCase;
import com.mranalizer.domain.port.out.AnalysisResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GetAnalysisResultsService implements GetAnalysisResultsUseCase {

    private final AnalysisResultRepository repository;

    public GetAnalysisResultsService(AnalysisResultRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AnalysisReport> getAllReports() {
        return repository.findAll();
    }

    @Override
    public Optional<AnalysisReport> getReport(Long reportId) {
        return repository.findById(reportId);
    }

    @Override
    public Optional<AnalysisResult> getResult(Long reportId, Long resultId) {
        return repository.findById(reportId)
                .flatMap(report -> report.getResults().stream()
                        .filter(r -> resultId.equals(r.getId()))
                        .findFirst());
    }
}
