package com.mranalizer.domain.service.project;

import com.mranalizer.domain.model.*;
import com.mranalizer.domain.model.project.*;
import com.mranalizer.domain.port.in.project.ProjectAnalysisUseCase;
import com.mranalizer.domain.port.out.LlmAnalyzer;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.ProjectAnalysisRepository;
import com.mranalizer.domain.rules.Rule;
import com.mranalizer.domain.scoring.ScoringEngine;
import com.mranalizer.domain.service.activity.ActivityAnalysisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ProjectAnalysisService implements ProjectAnalysisUseCase, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ProjectAnalysisService.class);
    private static final double[] HISTOGRAM_BOUNDARIES = {0.0, 0.2, 0.4, 0.6, 0.8, 1.0};

    private final ActivityAnalysisService activityService;
    private final MergeRequestProvider mergeRequestProvider;
    private final ScoringEngine scoringEngine;
    private final List<Rule> rules;
    private final ArtifactDetector artifactDetector;
    private final LlmAnalyzer llmAnalyzer;
    private final ProjectAnalysisRepository repository;
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final Set<String> inProgressSlugs = ConcurrentHashMap.newKeySet();

    public ProjectAnalysisService(ActivityAnalysisService activityService,
                                   MergeRequestProvider mergeRequestProvider,
                                   ScoringEngine scoringEngine,
                                   List<Rule> rules,
                                   ArtifactDetector artifactDetector,
                                   LlmAnalyzer llmAnalyzer,
                                   ProjectAnalysisRepository repository) {
        this.activityService = activityService;
        this.mergeRequestProvider = mergeRequestProvider;
        this.scoringEngine = scoringEngine;
        this.rules = rules;
        this.artifactDetector = artifactDetector;
        this.llmAnalyzer = llmAnalyzer;
        this.repository = repository;
    }

    @Override
    public ProjectAnalysisResult analyzeProject(String projectSlug, boolean useLlm) {
        return analyzeProject(projectSlug, useLlm, null);
    }

    @Override
    public ProjectAnalysisResult analyzeProject(String projectSlug, boolean useLlm,
                                                 BiConsumer<Integer, Integer> progressCallback) {
        // C4 fix: prevent concurrent analysis of the same repo
        if (!inProgressSlugs.add(projectSlug)) {
            throw new IllegalStateException("Analysis already in progress for " + projectSlug);
        }
        try {
            return doAnalyze(projectSlug, useLlm, progressCallback);
        } finally {
            inProgressSlugs.remove(projectSlug);
        }
    }

    private ProjectAnalysisResult doAnalyze(String projectSlug, boolean useLlm,
                                             BiConsumer<Integer, Integer> progressCallback) {
        log.info("Starting project analysis for {} (useLlm={})", projectSlug, useLlm);

        // 1. Get all PRs from activity cache
        var cache = activityService.getOrFetchCache(projectSlug);
        List<MergeRequest> allPrs = cache.getAllDetailedPrs();
        int total = allPrs.size();

        log.info("Analyzing {} PRs for {}", total, projectSlug);

        // 2. Parallel: per PR → fetchFiles + score + detect (with progress tracking)
        AtomicInteger processed = new AtomicInteger(0);
        Map<String, PrAnalysisRow> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = allPrs.stream()
                .map(mr -> CompletableFuture.runAsync(() -> {
                    PrAnalysisRow row = analyzeOnePr(projectSlug, mr, useLlm);
                    results.put(mr.getExternalId(), row);
                    int done = processed.incrementAndGet();
                    if (progressCallback != null) {
                        progressCallback.accept(done, total);
                    }
                }, executor))
                .toList();

        futures.forEach(CompletableFuture::join);

        // Preserve order from allPrs
        List<PrAnalysisRow> rows = allPrs.stream()
                .map(mr -> results.get(mr.getExternalId()))
                .filter(Objects::nonNull)
                .toList();

        // 3. Build summary
        ProjectSummary summary = buildSummary(rows);

        log.info("Project analysis complete for {}: {} PRs, {}% automatable, {}% BDD, {}% SDD",
                projectSlug, rows.size(),
                String.format("%.0f", summary.automatablePercent()),
                String.format("%.0f", summary.bddPercent()),
                String.format("%.0f", summary.sddPercent()));

        ProjectAnalysisResult result = new ProjectAnalysisResult(projectSlug, LocalDateTime.now(), rows, summary);
        return repository.save(result);
    }

    @Override
    public List<ProjectAnalysisResult> getSavedAnalyses(String projectSlug) {
        return repository.findByProjectSlug(projectSlug);
    }

    @Override
    public Optional<ProjectAnalysisResult> getSavedAnalysis(Long id) {
        return repository.findById(id);
    }

    @Override
    public void deleteAnalysis(Long id) {
        repository.deleteById(id);
        log.info("Deleted project analysis {}", id);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private PrAnalysisRow analyzeOnePr(String projectSlug, MergeRequest mr, boolean useLlm) {
        // 1. Fetch files
        List<ChangedFile> files = fetchFilesSafe(projectSlug, mr.getExternalId());

        // 2. Enrich MR with files for scoring (rules check changedFiles)
        MergeRequest mrWithFiles = enrichWithFiles(mr, files);

        // 3. Score
        LlmAssessment llm = useLlm ? analyzeLlmSafe(mrWithFiles) : new LlmAssessment(0.0, null, "none");
        AnalysisResult result = scoringEngine.evaluate(mrWithFiles, rules, llm);

        // 4. Detect BDD/SDD
        boolean hasBdd = artifactDetector.hasBdd(files);
        boolean hasSdd = artifactDetector.hasSdd(files);

        return new PrAnalysisRow(
                mr.getExternalId(), mr.getTitle(), mr.getAuthor(), mr.getState(), mr.getUrl(),
                mr.getCreatedAt(), mr.getMergedAt(),
                mr.getDiffStats().additions(), mr.getDiffStats().deletions(),
                result.getScore(), result.getVerdict(), result.getRuleResults(),
                result.getLlmComment(),
                hasBdd, hasSdd,
                artifactDetector.findBddFiles(files), artifactDetector.findSddFiles(files));
    }

    private List<ChangedFile> fetchFilesSafe(String projectSlug, String mrId) {
        try {
            return mergeRequestProvider.fetchFiles(projectSlug, mrId);
        } catch (Exception e) {
            log.warn("Failed to fetch files for PR #{}: {}", mrId, e.getMessage());
            return List.of();
        }
    }

    private LlmAssessment analyzeLlmSafe(MergeRequest mr) {
        try {
            return llmAnalyzer.analyze(mr);
        } catch (Exception e) {
            log.warn("LLM analysis failed for PR #{}: {}", mr.getExternalId(), e.getMessage());
            return new LlmAssessment(0.0, "LLM error: " + e.getMessage(), "error");
        }
    }

    private MergeRequest enrichWithFiles(MergeRequest mr, List<ChangedFile> files) {
        boolean hasTests = files.stream()
                .anyMatch(f -> f.path() != null && (f.path().contains("test") || f.path().contains("Test")
                        || f.path().contains("spec") || f.path().contains("Spec")));

        return MergeRequest.builder()
                .externalId(mr.getExternalId())
                .title(mr.getTitle())
                .description(mr.getDescription())
                .author(mr.getAuthor())
                .sourceBranch(mr.getSourceBranch())
                .targetBranch(mr.getTargetBranch())
                .state(mr.getState())
                .createdAt(mr.getCreatedAt())
                .mergedAt(mr.getMergedAt())
                .updatedAt(mr.getUpdatedAt())
                .labels(mr.getLabels())
                .changedFiles(files)
                .diffStats(mr.getDiffStats())
                .hasTests(hasTests)
                .ciPassed(mr.isCiPassed())
                .approvalsCount(mr.getApprovalsCount())
                .commentsCount(mr.getCommentsCount())
                .provider(mr.getProvider())
                .url(mr.getUrl())
                .projectSlug(mr.getProjectSlug())
                .build();
    }

    ProjectSummary buildSummary(List<PrAnalysisRow> rows) {
        int total = rows.size();
        if (total == 0) {
            return new ProjectSummary(0, 0, 0, 0, 0, 0, 0, 0,
                    List.of(), buildHistogram(rows), 0, 0, 0, 0);
        }

        int automatable = (int) rows.stream().filter(r -> r.aiVerdict() == Verdict.AUTOMATABLE).count();
        int maybe = (int) rows.stream().filter(r -> r.aiVerdict() == Verdict.MAYBE).count();
        int notSuitable = (int) rows.stream().filter(r -> r.aiVerdict() == Verdict.NOT_SUITABLE).count();

        double avgScore = rows.stream().mapToDouble(PrAnalysisRow::aiScore).average().orElse(0);

        List<RuleFrequency> topRules = rows.stream()
                .flatMap(r -> r.ruleResults().stream())
                .filter(RuleResult::matched)
                .collect(Collectors.groupingBy(RuleResult::ruleName))
                .entrySet().stream()
                .map(e -> new RuleFrequency(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream().mapToDouble(RuleResult::weight).average().orElse(0)))
                .sorted(Comparator.comparingInt(RuleFrequency::matchCount).reversed())
                .limit(10)
                .toList();

        List<ScoreHistogramBucket> histogram = buildHistogram(rows);

        int bddCount = (int) rows.stream().filter(PrAnalysisRow::hasBdd).count();
        int sddCount = (int) rows.stream().filter(PrAnalysisRow::hasSdd).count();

        return new ProjectSummary(total,
                automatable, maybe, notSuitable,
                pct(automatable, total), pct(maybe, total), pct(notSuitable, total),
                avgScore, topRules, histogram,
                bddCount, pct(bddCount, total),
                sddCount, pct(sddCount, total));
    }

    private List<ScoreHistogramBucket> buildHistogram(List<PrAnalysisRow> rows) {
        List<ScoreHistogramBucket> buckets = new ArrayList<>();
        for (int i = 0; i < HISTOGRAM_BOUNDARIES.length - 1; i++) {
            double start = HISTOGRAM_BOUNDARIES[i];
            double end = HISTOGRAM_BOUNDARIES[i + 1];
            boolean isLast = (i == HISTOGRAM_BOUNDARIES.length - 2);
            int count = (int) rows.stream()
                    .filter(r -> r.aiScore() >= start && (isLast ? r.aiScore() <= end : r.aiScore() < end))
                    .count();
            buckets.add(new ScoreHistogramBucket(start, end, count));
        }
        return buckets;
    }

    private double pct(int count, int total) {
        return total == 0 ? 0 : Math.round((double) count / total * 1000.0) / 10.0;
    }
}
