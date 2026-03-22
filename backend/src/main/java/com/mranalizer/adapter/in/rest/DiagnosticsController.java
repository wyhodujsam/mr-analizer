package com.mranalizer.adapter.in.rest;

import com.mranalizer.adapter.in.rest.dto.SqlStatsResponse;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diagnostics")
@Profile("dev")
public class DiagnosticsController {

    private final Statistics statistics;

    public DiagnosticsController(EntityManagerFactory emf) {
        this.statistics = emf.unwrap(SessionFactory.class).getStatistics();
        this.statistics.setStatisticsEnabled(true);
    }

    @GetMapping("/sql-stats")
    public SqlStatsResponse getSqlStats() {
        return new SqlStatsResponse(
                statistics.getQueryExecutionCount(),
                statistics.getQueryExecutionMaxTime(),
                statistics.getQueryExecutionMaxTimeQueryString(),
                statistics.getEntityLoadCount(),
                statistics.getEntityInsertCount(),
                statistics.getEntityUpdateCount(),
                statistics.getEntityDeleteCount(),
                statistics.getCollectionLoadCount(),
                statistics.getSecondLevelCacheHitCount(),
                statistics.getSecondLevelCacheMissCount(),
                statistics.getSessionOpenCount()
        );
    }

    @PostMapping("/sql-stats/reset")
    public ResponseEntity<Void> resetSqlStats() {
        statistics.clear();
        return ResponseEntity.noContent().build();
    }
}
