package com.mranalizer.adapter.in.rest.activity;

import com.mranalizer.adapter.in.rest.activity.dto.ActivityReportResponse;
import com.mranalizer.adapter.in.rest.activity.dto.ContributorResponse;
import com.mranalizer.domain.exception.InvalidRequestException;
import com.mranalizer.domain.model.activity.ActivityReport;
import com.mranalizer.domain.model.activity.ContributorInfo;
import com.mranalizer.domain.port.in.activity.ActivityAnalysisUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private static final Pattern SLUG_PART = Pattern.compile("[a-zA-Z0-9._-]+");
    private static final int MAX_AUTHOR_LENGTH = 100;

    private final ActivityAnalysisUseCase activityAnalysis;

    public ActivityController(ActivityAnalysisUseCase activityAnalysis) {
        this.activityAnalysis = activityAnalysis;
    }

    @GetMapping("/{owner}/{repo}/contributors")
    public ResponseEntity<List<ContributorResponse>> getContributors(
            @PathVariable String owner, @PathVariable String repo) {
        String projectSlug = validateSlug(owner, repo);
        List<ContributorInfo> contributors = activityAnalysis.getContributors(projectSlug);

        List<ContributorResponse> response = contributors.stream()
                .map(c -> new ContributorResponse(c.login(), c.prCount()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{owner}/{repo}/report")
    public ResponseEntity<ActivityReportResponse> getActivityReport(
            @PathVariable String owner, @PathVariable String repo,
            @RequestParam String author) {
        String projectSlug = validateSlug(owner, repo);
        validateAuthor(author);
        ActivityReport report = activityAnalysis.analyzeActivity(projectSlug, author);

        return ResponseEntity.ok(ActivityReportResponse.from(report));
    }

    private String validateSlug(String owner, String repo) {
        if (!SLUG_PART.matcher(owner).matches() || !SLUG_PART.matcher(repo).matches()) {
            throw new InvalidRequestException("Invalid repository slug: " + owner + "/" + repo);
        }
        return owner + "/" + repo;
    }

    private void validateAuthor(String author) {
        if (author == null || author.isBlank() || author.length() > MAX_AUTHOR_LENGTH) {
            throw new InvalidRequestException("Invalid author parameter");
        }
    }
}
