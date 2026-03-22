package com.mranalizer.adapter.in.rest.dto;

public record SqlStatsResponse(
        long queryExecutionCount,
        long queryExecutionMaxTime,
        String queryExecutionMaxTimeQuery,
        long entityLoadCount,
        long entityInsertCount,
        long entityUpdateCount,
        long entityDeleteCount,
        long collectionLoadCount,
        long secondLevelCacheHitCount,
        long secondLevelCacheMissCount,
        long sessionOpenCount
) {}
