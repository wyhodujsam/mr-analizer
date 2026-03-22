package com.mranalizer.adapter.out.provider.github;

import com.mranalizer.adapter.out.provider.github.dto.GitHubReview;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GitHubReviewAdapterTest {

    private GitHubClient gitHubClient;
    private GitHubReviewAdapter adapter;

    @BeforeEach
    void setUp() {
        gitHubClient = Mockito.mock(GitHubClient.class);
        adapter = new GitHubReviewAdapter(gitHubClient);
    }

    @Test
    void shouldMapReviewsCorrectly() {
        GitHubReview review = new GitHubReview();
        GitHubReview.User user = new GitHubReview.User();
        user.setLogin("reviewer1");
        review.setUser(user);
        review.setState("APPROVED");
        review.setSubmittedAt(ZonedDateTime.parse("2026-03-15T10:00:00Z"));

        when(gitHubClient.fetchReviews("owner", "repo", 42)).thenReturn(List.of(review));

        List<ReviewInfo> result = adapter.fetchReviews("owner/repo", "42");

        assertEquals(1, result.size());
        assertEquals("reviewer1", result.get(0).reviewer());
        assertEquals("APPROVED", result.get(0).state());
        assertNotNull(result.get(0).submittedAt());
    }

    @Test
    void shouldReturnEmptyForInvalidSlug() {
        List<ReviewInfo> result = adapter.fetchReviews("invalid", "42");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNullUserGracefully() {
        GitHubReview review = new GitHubReview();
        review.setState("COMMENTED");

        when(gitHubClient.fetchReviews("owner", "repo", 1)).thenReturn(List.of(review));

        List<ReviewInfo> result = adapter.fetchReviews("owner/repo", "1");

        assertEquals(1, result.size());
        assertEquals("unknown", result.get(0).reviewer());
    }
}
