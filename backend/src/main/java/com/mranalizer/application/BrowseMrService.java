package com.mranalizer.application;

import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.port.in.BrowseMrUseCase;
import com.mranalizer.domain.port.in.ManageReposUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BrowseMrService implements BrowseMrUseCase {

    private static final Logger log = LoggerFactory.getLogger(BrowseMrService.class);

    private final MergeRequestProvider provider;
    private final ManageReposUseCase manageReposUseCase;
    private final ConcurrentHashMap<String, List<MergeRequest>> cache = new ConcurrentHashMap<>();

    public BrowseMrService(MergeRequestProvider provider, ManageReposUseCase manageReposUseCase) {
        this.provider = provider;
        this.manageReposUseCase = manageReposUseCase;
    }

    @Override
    public List<MergeRequest> browse(FetchCriteria criteria) {
        return browse(criteria, false);
    }

    public List<MergeRequest> browse(FetchCriteria criteria, boolean forceRefresh) {
        String cacheKey = criteria.getProjectSlug();

        if (!forceRefresh && cache.containsKey(cacheKey)) {
            log.info("Returning cached browse results for {}", cacheKey);
            return cache.get(cacheKey);
        }

        log.info("Fetching fresh browse results for {}", cacheKey);
        List<MergeRequest> mergeRequests = provider.fetchMergeRequests(criteria);
        cache.put(cacheKey, mergeRequests);

        manageReposUseCase.add(criteria.getProjectSlug(), provider.getProviderName());
        return mergeRequests;
    }

    public void invalidateCache(String projectSlug) {
        cache.remove(projectSlug);
        log.info("Browse cache invalidated for {}", projectSlug);
    }

    public boolean hasCachedResults(String projectSlug) {
        return cache.containsKey(projectSlug);
    }
}
