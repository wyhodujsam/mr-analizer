package com.mranalizer.domain.port.out;

import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbound port: abstracts access to a VCS provider (GitLab, GitHub, …).
 * Pure domain — no framework dependencies.
 */
public interface MergeRequestProvider {

    /**
     * Fetches a list of merge requests matching the supplied criteria.
     *
     * @param criteria filtering parameters (project slug, branch, date range, etc.)
     * @return list of matching merge requests; never {@code null}
     */
    List<MergeRequest> fetchMergeRequests(FetchCriteria criteria);

    /**
     * Fetches a single merge request by its project and ID.
     *
     * @param projectSlug the project identifier understood by this provider
     * @param mrId        the merge request identifier
     * @return the merge request
     */
    MergeRequest fetchMergeRequest(String projectSlug, String mrId);

    /**
     * Fetches merge requests updated after the given timestamp.
     * Provider-agnostic: GitHub uses "since" param, GitLab uses "updated_after".
     * Returns PRs sorted by updatedAt desc, state=all, no limit (paginate all).
     */
    List<MergeRequest> fetchMergeRequestsUpdatedSince(String projectSlug, LocalDateTime updatedAfter);

    /** Returns the canonical name of this VCS provider (e.g. "gitlab", "github"). */
    String getProviderName();
}
