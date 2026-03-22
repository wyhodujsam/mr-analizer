package com.mranalizer.bdd.steps;

import com.mranalizer.domain.model.DiffStats;
import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityReport;
import com.mranalizer.domain.model.activity.ContributorInfo;
import com.mranalizer.domain.port.in.activity.ActivityAnalysisUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
import io.cucumber.java.Before;
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

public class ActivityAnalysisSteps {

    @Autowired
    private ActivityAnalysisUseCase activityAnalysis;

    @Autowired
    private MergeRequestProvider mergeRequestProvider;

    @Autowired
    private ReviewProvider reviewProvider;

    private List<MergeRequest> mockPrs = new ArrayList<>();
    private ActivityReport activityReport;
    private List<ContributorInfo> contributors;
    private String currentProjectSlug;

    @Before
    public void setUp() {
        mockPrs = new ArrayList<>();
        activityReport = null;
        contributors = null;
        currentProjectSlug = null;
    }

    @Given("repozytorium {string} z wieloma kontrybutorami")
    public void repozytoriumZWielomaKontrybutorami(String slug) {
        currentProjectSlug = slug;
    }

    @Given("kontrybutor {string} z {int} PR-ami")
    public void kontrybutorZPrAmi(String author, int count) {
        mockPrs.clear();
        // Monday 2026-03-09 at 10:00 — weekday, daytime
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 9, 10, 0);
        for (int i = 0; i < count; i++) {
            mockPrs.add(buildMr(String.valueOf(i + 1), author, 100, 20,
                    baseTime.plusHours(i), baseTime.plusHours(i).plusMinutes(60), "merged"));
        }
        setupMocks();
    }

    @Given("kontrybutor {string} z {int} PR-ami bez nieprawidłowości")
    public void kontrybutorBezNieprawidlowosci(String author, int count) {
        mockPrs.clear();
        // Use only weekdays (Mon-Fri), daytime, small PRs, 60-min review
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 9, 10, 0); // Monday
        int weekdayOffset = 0;
        for (int i = 0; i < count; i++) {
            LocalDateTime created = baseTime.plusDays(weekdayOffset);
            mockPrs.add(buildMr(String.valueOf(i + 1), author, 100, 20,
                    created, created.plusMinutes(60), "merged"));
            weekdayOffset++;
            // Skip weekends
            if (created.plusDays(1).getDayOfWeek().getValue() > 5) {
                weekdayOffset += 2;
            }
        }
        setupMocks();
        when(reviewProvider.fetchReviews(anyString(), anyString()))
                .thenReturn(List.of(new ReviewProvider.ReviewInfo("other-reviewer", "APPROVED", LocalDateTime.now())));
    }

    @And("{int} PR-y mają ponad {int} linii zmian")
    public void pryMajaPonadLiniiZmian(int count, int lines) {
        for (int i = 0; i < count && i < mockPrs.size(); i++) {
            MergeRequest original = mockPrs.get(i);
            mockPrs.set(i, buildMr(original.getExternalId(), original.getAuthor(),
                    lines + 100, 0, original.getCreatedAt(), original.getMergedAt(), original.getState()));
        }
        setupMocks();
    }

    @And("{int} PR został zmergowany w {int} minuty z {int} liniami zmian")
    public void prZmergowanyWMinuty(int count, int minutes, int lines) {
        int idx = mockPrs.size() - 1;
        MergeRequest original = mockPrs.get(idx);
        LocalDateTime created = original.getCreatedAt();
        mockPrs.set(idx, buildMr(original.getExternalId(), original.getAuthor(),
                lines, 0, created, created.plusMinutes(minutes), "merged"));
        setupMocks();
    }

    @And("{int} PR-y zostały utworzone w sobotę lub niedzielę")
    public void pryUtworzoneWWeekend(int count) {
        // Saturday 2026-03-07
        for (int i = 0; i < count && i < mockPrs.size(); i++) {
            MergeRequest original = mockPrs.get(i);
            LocalDateTime saturday = LocalDateTime.of(2026, 3, 7, 10 + i, 0);
            mockPrs.set(i, buildMr(original.getExternalId(), original.getAuthor(),
                    original.getDiffStats().additions(), original.getDiffStats().deletions(),
                    saturday, saturday.plusMinutes(60), original.getState()));
        }
        setupMocks();
    }

    @Given("kontrybutor {string} z PR numer {int} bez reviews")
    public void kontrybutorZPrBezReviews(String author, int prNumber) {
        mockPrs.clear();
        mockPrs.add(buildMr(String.valueOf(prNumber), author, 100, 20,
                LocalDateTime.of(2026, 3, 9, 10, 0),
                LocalDateTime.of(2026, 3, 9, 11, 0), "merged"));
        setupMocks();
        when(reviewProvider.fetchReviews(anyString(), eq(String.valueOf(prNumber)))).thenReturn(List.of());
    }

    @Given("kontrybutor {string} bez żadnych PR-ów")
    public void kontrybutorBezZadnychProw(String author) {
        mockPrs.clear();
        setupMocks();
    }

    @Given("kontrybutor {string} z PR-ami w dniach {string} i {string} i {string}")
    public void kontrybutorZPrAmiWDniach(String author, String date1, String date2, String date3) {
        mockPrs.clear();
        int id = 1;
        for (String dateStr : List.of(date1, date2, date3)) {
            LocalDate date = LocalDate.parse(dateStr);
            LocalDateTime created = date.atTime(10, 0);
            mockPrs.add(buildMr(String.valueOf(id++), author, 50, 10, created,
                    created.plusMinutes(30), "merged"));
        }
        setupMocks();
    }

    @Given("repozytorium {string} z PR-ami od {int} różnych autorów")
    public void repozytoriumZPramiOdRoznychAutorow(String slug, int authorCount) {
        currentProjectSlug = slug;
        mockPrs.clear();
        for (int i = 0; i < authorCount; i++) {
            String author = "author" + (i + 1);
            for (int j = 0; j < (i + 1); j++) {
                mockPrs.add(buildMr(String.valueOf(mockPrs.size() + 1), author, 50, 10,
                        LocalDateTime.of(2026, 3, 9, 10, 0),
                        LocalDateTime.of(2026, 3, 9, 11, 0), "merged"));
            }
        }
        setupMocks();
    }

    @When("analizuję aktywność {string}")
    public void analizujeAktywnosc(String author) {
        activityReport = activityAnalysis.analyzeActivity(
                currentProjectSlug != null ? currentProjectSlug : "owner/repo", author);
    }

    @When("pobieram listę kontrybutorów")
    public void pobieramListeKontributorow() {
        contributors = activityAnalysis.getContributors(
                currentProjectSlug != null ? currentProjectSlug : "owner/repo");
    }

    @When("klikam kwadrat {string} na heatmapie")
    public void klikamKwadratNaHeatmapie(String dateStr) {
        // Heatmap drill-down is frontend — backend test just verifies data exists
        assertNotNull(activityReport);
        LocalDate date = LocalDate.parse(dateStr);
        assertTrue(activityReport.getDailyActivity().containsKey(date));
    }

    @Then("widzę {int} PR-ów w statystykach")
    public void widzeProWStatystykach(int count) {
        assertEquals(count, activityReport.getStats().totalPrs());
    }

    @Then("widzę {int} flagi {string} z severity {word}")
    public void widzeFlagi(int count, String displayName, String severity) {
        long actual = activityReport.getFlags().stream()
                .filter(f -> f.type().getDisplayName().equals(displayName)
                        && f.severity().name().equalsIgnoreCase(severity))
                .count();
        assertEquals(count, actual, "Expected " + count + " flags '" + displayName + "' (" + severity + "), got " + actual);
    }

    @Then("widzę {int} flagę {string} z severity {word}")
    public void widzeFlagiSingular(int count, String displayName, String severity) {
        widzeFlagi(count, displayName, severity);
    }

    @Then("widzę flagę {string} z severity {word}")
    public void widzeFlageSingular(String displayName, String severity) {
        assertTrue(activityReport.getFlags().stream()
                .anyMatch(f -> f.type().getDisplayName().equals(displayName)
                        && f.severity().name().equalsIgnoreCase(severity)),
                "Expected flag '" + displayName + "' (" + severity + ")");
    }

    @Then("widzę flagę {string} z severity {word} na PR {int}")
    public void widzeFlagePrRef(String displayName, String severity, int prNumber) {
        assertTrue(activityReport.getFlags().stream()
                .anyMatch(f -> f.type().getDisplayName().equals(displayName)
                        && f.severity().name().equalsIgnoreCase(severity)
                        && ("#" + prNumber).equals(f.prReference())),
                "Expected flag '" + displayName + "' on PR #" + prNumber);
    }

    @Then("sekcja flag jest pusta")
    public void sekcjaFlagJestPusta() {
        assertTrue(activityReport.getFlags().isEmpty());
    }

    @Then("widzę komunikat braku aktywności")
    public void widzeKomunikatBrakuAktywnosci() {
        assertEquals(0, activityReport.getStats().totalPrs());
        assertFalse(activityReport.hasFlags());
    }

    @Then("otrzymuję {int} kontrybutorów z liczbą PR-ów")
    public void otrzymujeKontributorow(int count) {
        assertNotNull(contributors);
        assertEquals(count, contributors.size());
        contributors.forEach(c -> assertTrue(c.prCount() > 0));
    }

    @Then("heatmapa pokazuje {int} PR-y w dniu {string}")
    public void heatmapaPokazujePry(int count, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        assertTrue(activityReport.getDailyActivity().containsKey(date),
                "Expected daily activity for " + dateStr);
        assertEquals(count, activityReport.getDailyActivity().get(date).count());
    }

    @And("heatmapa pokazuje {int} PR w dniu {string}")
    public void heatmapaPokazujePrSingular(int count, String dateStr) {
        heatmapaPokazujePry(count, dateStr);
    }

    @Then("widzę listę {int} PR-ów z tego dnia z tytułami i rozmiarami")
    public void widzeListeProw(int count) {
        assertNotNull(activityReport);
        // Verify daily activity has an entry with the expected count
        boolean found = activityReport.getDailyActivity().values().stream()
                .anyMatch(da -> da.count() == count
                        && da.pullRequests().stream().allMatch(pr -> pr.title() != null && pr.size() >= 0));
        assertTrue(found, "Expected " + count + " PRs with titles and sizes in daily activity");
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
                .build();
    }
}
