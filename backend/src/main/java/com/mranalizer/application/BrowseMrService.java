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

    @Override
    public List<MergeRequest> browse(FetchCriteria criteria, boolean forceRefresh) {
        String cacheKey = criteria.cacheKey();

        if (forceRefresh) {
            cache.remove(cacheKey);
        }

        List<MergeRequest> result = cache.computeIfAbsent(cacheKey, key -> {
            log.info("Fetching fresh browse results for {}", criteria.getProjectSlug());
            return provider.fetchMergeRequests(criteria);
        });

        manageReposUseCase.add(criteria.getProjectSlug(), provider.getProviderName());
        return result;
    }

    @Override
    public void invalidateCache(String projectSlug) {
        cache.keySet().removeIf(key -> key.startsWith(projectSlug + "|"));
        log.info("Browse cache invalidated for {}", projectSlug);
    }

    @Override
    public boolean hasCachedResults(String projectSlug) {
        return cache.keySet().stream().anyMatch(key -> key.startsWith(projectSlug + "|"));
    }
}
