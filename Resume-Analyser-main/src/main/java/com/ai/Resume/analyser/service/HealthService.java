package com.ai.Resume.analyser.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HealthService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PerformanceMetricsService metricsService;

    public Map<String, Object> getHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        // Database check
        health.put("database", checkDatabase());

        // Memory usage
        health.put("memory", getMemoryUsage());

        // Cache stats
        health.put("cache", getCacheStats());

        // Analysis stats
        health.put("analysis", getAnalysisStats());

        return health;
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            db.put("status", "UP");
            db.put("type", "MySQL");
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
        }
        return db;
    }

    private Map<String, Object> getMemoryUsage() {
        Map<String, Object> memory = new LinkedHashMap<>();
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();

        memory.put("heapUsedMb", heap.getUsed() / (1024 * 1024));
        memory.put("heapMaxMb", heap.getMax() / (1024 * 1024));
        memory.put("heapUsagePercent", Math.round((double) heap.getUsed() / heap.getMax() * 100));
        memory.put("nonHeapUsedMb", nonHeap.getUsed() / (1024 * 1024));

        return memory;
    }

    private Map<String, Object> getCacheStats() {
        Map<String, Object> cache = new LinkedHashMap<>();
        Map<String, Object> metrics = metricsService.getMetrics();
        cache.put("hits", metrics.get("cacheHits"));
        cache.put("misses", metrics.get("cacheMisses"));
        cache.put("hitRatePercent", metrics.get("cacheHitRatePercent"));
        return cache;
    }

    private Map<String, Object> getAnalysisStats() {
        Map<String, Object> analysis = new LinkedHashMap<>();
        Map<String, Object> metrics = metricsService.getMetrics();
        analysis.put("totalAnalyses", metrics.get("totalAnalyses"));
        analysis.put("avgTimeMs", metrics.get("avgAnalysisTimeMs"));
        analysis.put("aiCalls", metrics.get("totalAiCalls"));
        analysis.put("aiFailures", metrics.get("aiFailures"));
        return analysis;
    }
}
