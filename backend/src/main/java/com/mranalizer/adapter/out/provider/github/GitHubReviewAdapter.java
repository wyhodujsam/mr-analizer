package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.adapter.out.provider.github.dto.GitHubReview;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class GitHubReviewAdapter implements ReviewProvider {

    private final GitHubClient gitHubClient;

    public GitHubReviewAdapter(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    @Override
    public List<ReviewInfo> fetchReviews(String projectSlug, String prId) {
        String[] parts = projectSlug.split("/");
        if (parts.length != 2) {
            return List.of();
        }

        int prNumber = Integer.parseInt(prId);
        List<GitHubReview> reviews = gitHubClient.fetchReviews(parts[0], parts[1], prNumber);

        return reviews.stream()
                .map(this::toReviewInfo)
                .toList();
    }

    private ReviewInfo toReviewInfo(GitHubReview review) {
        String reviewer = review.getUser() != null ? review.getUser().getLogin() : "unknown";
        String state = review.getState() != null ? review.getState() : "COMMENTED";
        LocalDateTime submittedAt = review.getSubmittedAt() != null
                ? review.getSubmittedAt().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
                : null;

        return new ReviewInfo(reviewer, state, submittedAt);
    }
}
