package com.mranalizer.domain.port.in.activity;

import com.mranalizer.domain.model.activity.ActivityReport;
import com.mranalizer.domain.model.activity.ContributorInfo;

import java.util.List;

public interface ActivityAnalysisUseCase {

    List<ContributorInfo> getContributors(String projectSlug);

    ActivityReport analyzeActivity(String projectSlug, String author);
}
