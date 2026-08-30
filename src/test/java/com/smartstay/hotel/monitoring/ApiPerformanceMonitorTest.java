package com.smartstay.hotel.monitoring;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiPerformanceMonitorTest {
    @Test
    void snapshotCalculatesPercentilesErrorsAndNormalizesResourceIds() {
        ApiPerformanceMonitor monitor = new ApiPerformanceMonitor(100, 15, 500, 0.01);
        monitor.record("GET", "/api/booking/101", 200, 20);
        monitor.record("GET", "/api/booking/202", 200, 40);
        monitor.record("POST", "/api/booking/create", 500, 800);
        monitor.record("GET", "/api/room/list", 200, 60);

        Map<String, Object> snapshot = monitor.snapshot();

        assertEquals(4, snapshot.get("sampleCount"));
        assertEquals(40L, snapshot.get("p50Ms"));
        assertEquals(800L, snapshot.get("p95Ms"));
        assertEquals(0.25, snapshot.get("errorRate"));
        assertEquals("WARNING", snapshot.get("health"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> endpoints = (List<Map<String, Object>>) snapshot.get("endpoints");
        Map<String, Object> normalized = endpoints.stream()
                .filter(row -> "GET /api/booking/{id}".equals(row.get("endpoint")))
                .findFirst().orElseThrow();
        assertEquals(2, normalized.get("count"));
    }

    @Test
    void emptyWindowIsReportedAsNoDataInsteadOfHealthy() {
        ApiPerformanceMonitor monitor = new ApiPerformanceMonitor(100, 15, 500, 0.01);

        Map<String, Object> snapshot = monitor.snapshot();

        assertEquals("NO_DATA", snapshot.get("health"));
        assertEquals(0L, snapshot.get("p95Ms"));
        assertEquals(0, snapshot.get("sampleCount"));
    }
}
