package com.mranalizer.domain.port.out.activity;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewProvider {

    List<ReviewInfo> fetchReviews(String projectSlug, String prId);

    record ReviewInfo(
            String reviewer,
            String state,
            LocalDateTime submittedAt
    ) {
    }
}
