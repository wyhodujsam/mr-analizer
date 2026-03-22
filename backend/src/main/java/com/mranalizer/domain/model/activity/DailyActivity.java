package com.mranalizer.domain.model.activity;

import java.time.LocalDate;
import java.util.List;

public record DailyActivity(
        LocalDate date,
        int count,
        List<PrSummary> pullRequests
) {

    public record PrSummary(
            String id,
            String title,
            int size,
            List<ActivityFlag> flags
    ) {
    }
}
