package com.mranalizer.adapter.in.rest.project;

import com.mranalizer.adapter.in.rest.project.dto.ProjectAnalysisResponse;
import com.mranalizer.domain.exception.InvalidRequestException;
import com.mranalizer.domain.exception.ReportNotFoundException;
import com.mranalizer.domain.model.project.ProjectAnalysisResult;
import com.mranalizer.domain.port.in.project.ProjectAnalysisUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/project")
public class ProjectAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(ProjectAnalysisController.class);
    private static final Pattern SLUG_PART = Pattern.compile("[a-zA-Z0-9._-]+");
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    private final ProjectAnalysisUseCase projectAnalysis;
    private final ObjectMapper objectMapper;
    private final ExecutorService sseExecutor = Executors.newFixedThreadPool(4);

    public ProjectAnalysisController(ProjectAnalysisUseCase projectAnalysis, ObjectMapper objectMapper) {
        this.projectAnalysis = projectAnalysis;
        this.objectMapper = objectMapper;
    }

    @PreDestroy
    public void shutdown() {
        sseExecutor.shutdown();
    }

    @PostMapping("/{owner}/{repo}/analyze")
    public ResponseEntity<ProjectAnalysisResponse> analyzeProject(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "false") boolean useLlm) {
        String projectSlug = validateSlug(owner, repo);
        ProjectAnalysisResult result = projectAnalysis.analyzeProject(projectSlug, useLlm);
        return ResponseEntity.ok(ProjectAnalysisResponse.from(result));
    }

    @PostMapping(value = "/{owner}/{repo}/analyze-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeProjectStream(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "false") boolean useLlm) {
        String projectSlug = validateSlug(owner, repo);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean clientDisconnected = new AtomicBoolean(false);

        emitter.onTimeout(() -> clientDisconnected.set(true));
        emitter.onCompletion(() -> clientDisconnected.set(true));
        emitter.onError(e -> clientDisconnected.set(true));

        sseExecutor.submit(() -> {
            try {
                ProjectAnalysisResult result = projectAnalysis.analyzeProject(projectSlug, useLlm,
                        (processed, total) -> {
                            if (clientDisconnected.get()) return;
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("progress")
                                        .data("{\"processed\":" + processed + ",\"total\":" + total + "}"));
                            } catch (Exception e) {
                                clientDisconnected.set(true);
                            }
                        });

                if (!clientDisconnected.get()) {
                    String json = objectMapper.writeValueAsString(ProjectAnalysisResponse.from(result));
                    emitter.send(SseEmitter.event().name("result").data(json));
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE analysis error for {}: {}", projectSlug, e.getMessage());
                if (!clientDisconnected.get()) {
                    emitter.completeWithError(e);
                }
            }
        });

        return emitter;
    }

    @GetMapping("/{owner}/{repo}/analyses")
    public ResponseEntity<List<ProjectAnalysisResponse>> getSavedAnalyses(
            @PathVariable String owner, @PathVariable String repo) {
        String projectSlug = validateSlug(owner, repo);
        List<ProjectAnalysisResponse> responses = projectAnalysis.getSavedAnalyses(projectSlug).stream()
                .map(ProjectAnalysisResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/analyses/{id}")
    public ResponseEntity<ProjectAnalysisResponse> getSavedAnalysis(@PathVariable Long id) {
        ProjectAnalysisResult result = projectAnalysis.getSavedAnalysis(id)
                .orElseThrow(() -> new ReportNotFoundException("Project analysis not found: " + id));
        return ResponseEntity.ok(ProjectAnalysisResponse.from(result));
    }

    @DeleteMapping("/analyses/{id}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Long id) {
        projectAnalysis.deleteAnalysis(id);
        return ResponseEntity.noContent().build();
    }

    private String validateSlug(String owner, String repo) {
        if (!SLUG_PART.matcher(owner).matches() || !SLUG_PART.matcher(repo).matches()) {
            throw new InvalidRequestException("Invalid repository slug: " + owner + "/" + repo);
        }
        return owner + "/" + repo;
    }
}
