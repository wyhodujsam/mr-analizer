package com.mranalizer.application;

import com.mranalizer.domain.model.FetchCriteria;
import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.port.in.BrowseMrUseCase;
import com.mranalizer.domain.port.in.ManageReposUseCase;
import com.mranalizer.domain.port.out.MergeRequestProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrowseMrService implements BrowseMrUseCase {

    private final MergeRequestProvider provider;
    private final ManageReposUseCase manageReposUseCase;

    public BrowseMrService(MergeRequestProvider provider, ManageReposUseCase manageReposUseCase) {
        this.provider = provider;
        this.manageReposUseCase = manageReposUseCase;
    }

    @Override
    public List<MergeRequest> browse(FetchCriteria criteria) {
        List<MergeRequest> mergeRequests = provider.fetchMergeRequests(criteria);
        // Auto-save the repository when browsing
        manageReposUseCase.add(criteria.getProjectSlug(), provider.getProviderName());
        return mergeRequests;
    }
}
