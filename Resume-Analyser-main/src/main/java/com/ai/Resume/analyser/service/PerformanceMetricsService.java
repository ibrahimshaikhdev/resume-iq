package com.ai.Resume.analyser.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PerformanceMetricsService {

    private final AtomicLong totalAnalysisTimeMs = new AtomicLong(0);
    private final AtomicInteger analysisCount = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    private final AtomicInteger aiCallCount = new AtomicInteger(0);
    private final AtomicInteger aiFailureCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicLong> endpointTimes = new ConcurrentHashMap<>();

    public void recordAnalysisTime(long durationMs) {
        totalAnalysisTimeMs.addAndGet(durationMs);
        analysisCount.incrementAndGet();
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void recordAiCall() {
        aiCallCount.incrementAndGet();
    }

    public void recordAiFailure() {
        aiFailureCount.incrementAndGet();
    }

    public void recordEndpointTime(String endpoint, long durationMs) {
        endpointTimes.computeIfAbsent(endpoint, k -> new AtomicLong(0)).addAndGet(durationMs);
    }

    public Map<String, Object> getMetrics() {
        int totalCacheRequests = cacheHits.get() + cacheMisses.get();
        double cacheHitRate = totalCacheRequests > 0
                ? (double) cacheHits.get() / totalCacheRequests * 100
                : 0;

        double avgAnalysisTime = analysisCount.get() > 0
                ? (double) totalAnalysisTimeMs.get() / analysisCount.get()
                : 0;

        return Map.of(
                "totalAnalyses", analysisCount.get(),
                "avgAnalysisTimeMs", Math.round(avgAnalysisTime),
                "totalAiCalls", aiCallCount.get(),
                "aiFailures", aiFailureCount.get(),
                "cacheHits", cacheHits.get(),
                "cacheMisses", cacheMisses.get(),
                "cacheHitRatePercent", Math.round(cacheHitRate * 100.0) / 100.0,
                "endpointTimings", endpointTimes.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().get()
                        ))
        );
    }

    public void reset() {
        totalAnalysisTimeMs.set(0);
        analysisCount.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        aiCallCount.set(0);
        aiFailureCount.set(0);
        endpointTimes.clear();
    }
}
