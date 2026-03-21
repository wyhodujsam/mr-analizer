package com.mranalizer.domain.port.in;

import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;

import java.util.List;

/**
 * Inbound port: browse merge requests from a VCS provider without running analysis.
 */
public interface BrowseMrUseCase {

    List<MergeRequest> browse(FetchCriteria criteria);

    List<MergeRequest> browse(FetchCriteria criteria, boolean forceRefresh);

    void invalidateCache(String projectSlug);

    boolean hasCachedResults(String projectSlug);
}
