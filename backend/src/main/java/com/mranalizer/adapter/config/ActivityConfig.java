package com.mranalizer.adapter.config;

import com.mranalizer.domain.port.out.MergeRequestProvider;
import com.mranalizer.domain.port.out.activity.ReviewProvider;
import com.mranalizer.domain.service.activity.ActivityAnalysisService;
import com.mranalizer.domain.service.activity.rules.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ActivityConfig {

    @Bean
    public List<ActivityRule> activityRules() {
        return List.of(
                new LargePrRule(),
                new QuickReviewRule(),
                new WeekendWorkRule(),
                new NightWorkRule(),
                new NoReviewRule(),
                new SelfMergeRule()
        );
    }

    @Bean
    public AggregateRules aggregateRules() {
        return new AggregateRules();
    }

    @Bean
    public ActivityAnalysisService activityAnalysisService(
            MergeRequestProvider mergeRequestProvider,
            ReviewProvider reviewProvider,
            List<ActivityRule> activityRules,
            AggregateRules aggregateRules) {
        return new ActivityAnalysisService(mergeRequestProvider, reviewProvider, activityRules, aggregateRules);
    }
}
