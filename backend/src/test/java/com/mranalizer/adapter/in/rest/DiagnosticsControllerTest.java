package com.mranalizer.adapter.in.rest;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiagnosticsControllerTest {

    private Statistics statistics;
    private DiagnosticsController controller;

    @BeforeEach
    void setUp() {
        statistics = mock(Statistics.class);
        SessionFactory sessionFactory = mock(SessionFactory.class);
        EntityManagerFactory emf = mock(EntityManagerFactory.class);

        when(emf.unwrap(SessionFactory.class)).thenReturn(sessionFactory);
        when(sessionFactory.getStatistics()).thenReturn(statistics);

        controller = new DiagnosticsController(emf);
    }

    @Test
    void getSqlStats_returnsAllStatistics() {
        when(statistics.getQueryExecutionCount()).thenReturn(42L);
        when(statistics.getQueryExecutionMaxTime()).thenReturn(150L);
        when(statistics.getQueryExecutionMaxTimeQueryString()).thenReturn("select * from analysis_result");
        when(statistics.getEntityLoadCount()).thenReturn(100L);
        when(statistics.getEntityInsertCount()).thenReturn(10L);
        when(statistics.getEntityUpdateCount()).thenReturn(5L);
        when(statistics.getEntityDeleteCount()).thenReturn(2L);
        when(statistics.getCollectionLoadCount()).thenReturn(30L);
        when(statistics.getSecondLevelCacheHitCount()).thenReturn(8L);
        when(statistics.getSecondLevelCacheMissCount()).thenReturn(3L);
        when(statistics.getSessionOpenCount()).thenReturn(50L);

        var response = controller.getSqlStats();

        assertEquals(42L, response.queryExecutionCount());
        assertEquals(150L, response.queryExecutionMaxTime());
        assertEquals("select * from analysis_result", response.queryExecutionMaxTimeQuery());
        assertEquals(100L, response.entityLoadCount());
        assertEquals(10L, response.entityInsertCount());
        assertEquals(5L, response.entityUpdateCount());
        assertEquals(2L, response.entityDeleteCount());
        assertEquals(30L, response.collectionLoadCount());
        assertEquals(8L, response.secondLevelCacheHitCount());
        assertEquals(3L, response.secondLevelCacheMissCount());
        assertEquals(50L, response.sessionOpenCount());
    }

    @Test
    void resetSqlStats_clearsAndReturns204() {
        var response = controller.resetSqlStats();

        verify(statistics).clear();
        assertEquals(204, response.getStatusCode().value());
    }
}
