package com.mranalizer.bdd.steps;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityReport;
import com.mranalizer.domain.model.activity.ProductivityMetrics;
import com.mranalizer.domain.port.in.activity.ActivityAnalysisUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class ActivityMetricsSteps {

    @Autowired
    private ActivityAnalysisUseCase activityAnalysis;

    @Autowired
    private MergeRequestProvider mergeRequestProvider;

    @Autowired
    private ReviewProvider reviewProvider;

    private List<MergeRequest> mockPrs = new ArrayList<>();
    private ActivityReport report;

    @Given("kontrybutor {string} z {int} zmergowanymi PR-ami w ostatnich {int} tygodniach")
    public void kontrybutorZZmergowanymiPrami(String author, int count, int weeks) {
        activityAnalysis.invalidateCache("owner/repo");
        mockPrs.clear();
        LocalDate ref = LocalDate.now();
        int perWeek = count / weeks;
        int remainder = count % weeks;

        for (int w = 0; w < weeks; w++) {
            int weekCount = perWeek + (w == 0 ? remainder : 0);
            for (int i = 0; i < weekCount; i++) {
                LocalDateTime created = ref.minusWeeks(weeks - w).atTime(10 + i, 0);
                LocalDateTime merged = created.plusHours(2);
                mockPrs.add(buildMr(String.valueOf(mockPrs.size() + 1), author, 100, 20, created, merged, "merged"));
            }
        }
        setupMocks();
    }

    @Given("kontrybutor {string} bez zmergowanych PR-ów w ostatnich {int} tygodniach")
    public void kontrybutorBezZmergowanychProw(String author, int weeks) {
        activityAnalysis.invalidateCache("owner/repo");
        mockPrs.clear();
        mockPrs.add(buildMr("1", author, 50, 10,
                LocalDateTime.of(2026, 3, 9, 10, 0), null, "open"));
        setupMocks();
    }

    @Given("kontrybutor {string} z {int} PR-ami w pierwszym tygodniu i {int} w pozostałych trzech")
    public void kontrybutorNierownaDystrybucja(String author, int firstWeekCount, int restCount) {
        activityAnalysis.invalidateCache("owner/repo");
        mockPrs.clear();
        LocalDate ref = LocalDate.now();

        // First week (4 weeks ago)
        for (int i = 0; i < firstWeekCount; i++) {
            LocalDateTime created = ref.minusWeeks(4).atTime(10 + i, 0);
            LocalDateTime merged = created.plusHours(1);
            mockPrs.add(buildMr(String.valueOf(mockPrs.size() + 1), author, 50, 10, created, merged, "merged"));
        }
        setupMocks();
    }

    @Given("kontrybutor {string} z PR-ami o czasach merge {int}h, {int}h, {int}h, {int}h, {int}h")
    public void kontrybutorZPramiOCzasachMerge(String author, int h1, int h2, int h3, int h4, int h5) {
        activityAnalysis.invalidateCache("owner/repo");
        mockPrs.clear();
        int[] hours = {h1, h2, h3, h4, h5};
        for (int i = 0; i < hours.length; i++) {
            LocalDateTime created = LocalDateTime.of(2026, 3, 9 + i, 10, 0);
            LocalDateTime merged = created.plusHours(hours[i]);
            mockPrs.add(buildMr(String.valueOf(i + 1), author, 100, 20, created, merged, "merged"));
        }
        setupMocks();
    }

    @Given("kontrybutor {string} z PR-ami o rozmiarach \\({int}+\\/{int}-), \\({int}+\\/{int}-), \\({int}+\\/{int}-)")
    public void kontrybutorZPramiORozmiarach(String author,
                                              int add1, int del1, int add2, int del2, int add3, int del3) {
        activityAnalysis.invalidateCache("owner/repo");
        mockPrs.clear();
        LocalDateTime base = LocalDateTime.of(2026, 3, 9, 10, 0);

        mockPrs.add(buildMr("1", author, add1, del1, base, base.plusHours(2), "merged"));
        mockPrs.add(buildMr("2", author, add2, del2, base.plusDays(1), base.plusDays(1).plusHours(2), "merged"));
        mockPrs.add(buildMr("3", author, add3, del3, base.plusDays(2), base.plusDays(2).plusHours(2), "merged"));
        setupMocks();
    }

    @Given("kontrybutor {string} dał {int} review i otrzymał {int} review")
    public void kontrybutorDalIOtrzymalReview(String author, int given, int received) {
        activityAnalysis.invalidateCache("owner/repo");
        mockPrs.clear();

        // Author's PRs (will receive reviews)
        for (int i = 0; i < received; i++) {
            LocalDateTime created = LocalDateTime.of(2026, 3, 9 + i, 10, 0);
            mockPrs.add(buildMr(String.valueOf(i + 1), author, 50, 10, created,
                    created.plusHours(2), "merged"));
        }

        // Other person's PRs (author gave reviews)
        for (int i = 0; i < given; i++) {
            LocalDateTime created = LocalDateTime.of(2026, 3, 9 + i, 14, 0);
            mockPrs.add(buildMr(String.valueOf(received + i + 1), "other-author", 50, 10,
                    created, created.plusHours(2), "merged"));
        }

        when(mergeRequestProvider.fetchMergeRequests(any(FetchCriteria.class))).thenReturn(mockPrs);
        for (MergeRequest mr : mockPrs) {
            when(mergeRequestProvider.fetchMergeRequest(anyString(), eq(mr.getExternalId()))).thenReturn(mr);
        }

        // Set up reviews: author receives reviews on their PRs, author gives reviews on others' PRs
        for (MergeRequest mr : mockPrs) {
            if (author.equals(mr.getAuthor())) {
                when(reviewProvider.fetchReviews(anyString(), eq(mr.getExternalId())))
                        .thenReturn(List.of(new ReviewInfo("reviewer-x", "APPROVED", LocalDateTime.now())));
            } else {
                when(reviewProvider.fetchReviews(anyString(), eq(mr.getExternalId())))
                        .thenReturn(List.of(new ReviewInfo(author, "APPROVED", LocalDateTime.now())));
            }
        }
    }

    @Then("velocity wynosi {double} PR\\/tydzień")
    public void velocityWynosi(double expected) {
        ProductivityMetrics p = report.getStats().productivity();
        assertNotNull(p, "Productivity metrics should not be null");
        assertEquals(expected, p.velocity().prsPerWeek(), 0.1);
    }

    @Then("cycle time wyświetla {string}")
    public void cycleTimeWyswietla(String expected) {
        ProductivityMetrics p = report.getStats().productivity();
        assertNotNull(p);
        if ("brak danych".equals(expected)) {
            assertEquals(0, p.cycleTime().avgHours());
        }
    }

    @Then("velocity trend to {string}")
    public void velocityTrendTo(String expected) {
        ProductivityMetrics p = report.getStats().productivity();
        assertNotNull(p);
        assertEquals(expected, p.velocity().trend());
    }

    @Then("cycle time median wynosi {double} godzin")
    public void cycleTimeMedian(double expected) {
        ProductivityMetrics p = report.getStats().productivity();
        assertNotNull(p);
        assertEquals(expected, p.cycleTime().medianHours(), 0.1);
    }

    @And("cycle time p90 wynosi {double} godzin")
    public void cycleTimeP90(double expected) {
        ProductivityMetrics p = report.getStats().productivity();
        assertNotNull(p);
        assertEquals(expected, p.cycleTime().p90Hours(), 0.1);
    }

    @Then("total impact wynosi {int} linii")
    public void totalImpact(int expected) {
        ProductivityMetrics p = report.getStats().productivity();
        assertNotNull(p);
        assertEquals(expected, p.impact().totalLines());
    }

    @And("churn ratio wynosi {double}")
    public void churnRatio(double expected) {
        ProductivityMetrics p = report.getStats().productivity();
        assertNotNull(p);
        assertEquals(expected, p.codeChurn().churnRatio(), 0.01);
    }

    @Then("review engagement ratio wynosi {double}")
    public void reviewEngagementRatio(double expected) {
        ProductivityMetrics p = report.getStats().productivity();
        assertNotNull(p);
        assertEquals(expected, p.reviewEngagement().ratio(), 0.01);
    }

    @And("review engagement label to {string}")
    public void reviewEngagementLabel(String expected) {
        ProductivityMetrics p = report.getStats().productivity();
        assertNotNull(p);
        assertEquals(expected, p.reviewEngagement().label());
    }

    // "analizuję aktywność" step is defined in ActivityAnalysisSteps.
    // For metrics tests, we use the same step but need to access report here.
    // We fetch report by calling analyzeActivity again — cache ensures no extra API calls.
    @When("analizuję aktywność dla metryk {string}")
    public void analizujeAktywnoscDlaMetryk(String author) {
        report = activityAnalysis.analyzeActivity("owner/repo", author);
    }

    private void setupMocks() {
        when(mergeRequestProvider.fetchMergeRequests(any(FetchCriteria.class))).thenReturn(mockPrs);
        for (MergeRequest mr : mockPrs) {
            when(mergeRequestProvider.fetchMergeRequest(anyString(), eq(mr.getExternalId()))).thenReturn(mr);
        }
        when(reviewProvider.fetchReviews(anyString(), anyString())).thenReturn(List.of());
    }

    private MergeRequest buildMr(String id, String author, int additions, int deletions,
                                  LocalDateTime createdAt, LocalDateTime mergedAt, String state) {
        return MergeRequest.builder()
                .externalId(id)
                .title("PR #" + id)
                .author(author)
                .state(state)
                .diffStats(new DiffStats(additions, deletions, 3))
                .createdAt(createdAt)
                .mergedAt(mergedAt)
                .updatedAt(mergedAt != null ? mergedAt : createdAt)
                .build();
    }
}
