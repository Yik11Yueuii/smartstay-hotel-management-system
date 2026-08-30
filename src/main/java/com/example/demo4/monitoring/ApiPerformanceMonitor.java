package com.example.demo4.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ApiPerformanceMonitor {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Deque<ApiSample> samples = new ArrayDeque<>();
    private final Instant startedAt = Instant.now();
    private final int maxSamples;
    private final Duration window;
    private final long slowThresholdMs;
    private final double errorRateThreshold;

    public ApiPerformanceMonitor(
            @Value("${monitoring.api.max-samples:5000}") int maxSamples,
            @Value("${monitoring.api.window-minutes:15}") long windowMinutes,
            @Value("${monitoring.api.slow-threshold-ms:500}") long slowThresholdMs,
            @Value("${monitoring.api.error-rate-threshold:0.01}") double errorRateThreshold) {
        this.maxSamples = Math.max(100, maxSamples);
        this.window = Duration.ofMinutes(Math.max(1, windowMinutes));
        this.slowThresholdMs = Math.max(1, slowThresholdMs);
        this.errorRateThreshold = Math.max(0, errorRateThreshold);
    }

    public synchronized void record(String method, String requestPath, int status, long durationMs) {
        Instant now = Instant.now();
        evictExpired(now);
        samples.addLast(new ApiSample(now, method, normalizePath(requestPath), status, Math.max(0, durationMs)));
        while (samples.size() > maxSamples) samples.removeFirst();
    }

    public synchronized Map<String, Object> snapshot() {
        Instant now = Instant.now();
        evictExpired(now);
        List<ApiSample> current = new ArrayList<>(samples);
        long errors = current.stream().filter(ApiSample::isError).count();
        double errorRate = rate(errors, current.size());
        long p50 = percentile(current.stream().map(ApiSample::durationMs).collect(Collectors.toList()), 0.50);
        long p95 = percentile(current.stream().map(ApiSample::durationMs).collect(Collectors.toList()), 0.95);
        long p99 = percentile(current.stream().map(ApiSample::durationMs).collect(Collectors.toList()), 0.99);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("windowMinutes", window.toMinutes());
        result.put("sampleCount", current.size());
        result.put("p50Ms", p50);
        result.put("p95Ms", p95);
        result.put("p99Ms", p99);
        result.put("errorRate", round(errorRate));
        result.put("slowThresholdMs", slowThresholdMs);
        result.put("errorRateThreshold", errorRateThreshold);
        result.put("health", current.isEmpty() ? "NO_DATA"
                : p95 > slowThresholdMs || errorRate > errorRateThreshold ? "WARNING" : "HEALTHY");
        result.put("uptimeMinutes", Duration.between(startedAt, now).toMinutes());
        result.put("endpoints", endpointMetrics(current));
        result.put("trend", trend(current, now));
        return result;
    }

    static String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        return path.replaceAll("/\\d+(?=/|$)", "/{id}");
    }

    private List<Map<String, Object>> endpointMetrics(List<ApiSample> current) {
        Map<String, List<ApiSample>> grouped = current.stream()
                .collect(Collectors.groupingBy(sample -> sample.method() + " " + sample.path()));
        return grouped.entrySet().stream().map(entry -> {
                    List<ApiSample> endpointSamples = entry.getValue();
                    long errors = endpointSamples.stream().filter(ApiSample::isError).count();
                    List<Long> durations = endpointSamples.stream().map(ApiSample::durationMs).collect(Collectors.toList());
                    Map<String, Object> row = new HashMap<>();
                    row.put("endpoint", entry.getKey());
                    row.put("count", endpointSamples.size());
                    row.put("p95Ms", percentile(durations, 0.95));
                    row.put("maxMs", durations.stream().mapToLong(Long::longValue).max().orElse(0));
                    row.put("errorRate", round(rate(errors, endpointSamples.size())));
                    return row;
                })
                .sorted(Comparator.<Map<String, Object>, Long>comparing(row -> (Long) row.get("p95Ms"))
                        .reversed().thenComparing(row -> (String) row.get("endpoint")))
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> trend(List<ApiSample> current, Instant now) {
        Instant end = now.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES);
        int bucketCount = (int) Math.min(6, Math.max(1, (window.toMinutes() + 4) / 5));
        Instant start = end.minus(bucketCount * 5L, ChronoUnit.MINUTES);
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            Instant bucketStart = start.plus(i * 5L, ChronoUnit.MINUTES);
            Instant bucketEnd = bucketStart.plus(5, ChronoUnit.MINUTES);
            List<ApiSample> bucket = current.stream()
                    .filter(sample -> !sample.time().isBefore(bucketStart) && sample.time().isBefore(bucketEnd))
                    .collect(Collectors.toList());
            long errors = bucket.stream().filter(ApiSample::isError).count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", TIME_FORMAT.format(bucketStart));
            point.put("count", bucket.size());
            point.put("p95Ms", percentile(bucket.stream().map(ApiSample::durationMs).collect(Collectors.toList()), 0.95));
            point.put("errorRate", round(rate(errors, bucket.size())));
            points.add(point);
        }
        return points;
    }

    private void evictExpired(Instant now) {
        Instant cutoff = now.minus(window);
        while (!samples.isEmpty() && samples.peekFirst().time().isBefore(cutoff)) samples.removeFirst();
    }

    private static long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0;
        values.sort(Long::compareTo);
        int index = Math.max(0, (int) Math.ceil(values.size() * percentile) - 1);
        return values.get(index);
    }

    private static double rate(long numerator, long denominator) {
        return denominator == 0 ? 0 : (double) numerator / denominator;
    }

    private static double round(double value) {
        return Double.parseDouble(String.format(Locale.ROOT, "%.4f", value));
    }

    private record ApiSample(Instant time, String method, String path, int status, long durationMs) {
        boolean isError() {
            return status >= 500;
        }
    }
}
