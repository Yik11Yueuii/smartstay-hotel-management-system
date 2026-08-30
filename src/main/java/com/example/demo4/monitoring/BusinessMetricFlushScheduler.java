package com.example.demo4.monitoring;

import com.example.demo4.service.BusinessMetricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "monitoring.business-metrics.flush-enabled",
        havingValue = "true", matchIfMissing = true)
public class BusinessMetricFlushScheduler {
    private static final Logger log = LoggerFactory.getLogger(BusinessMetricFlushScheduler.class);
    private final BusinessMetricService metricService;

    public BusinessMetricFlushScheduler(BusinessMetricService metricService) {
        this.metricService = metricService;
    }

    @Scheduled(initialDelayString = "${monitoring.business-metrics.initial-delay-ms:5000}",
            fixedDelayString = "${monitoring.business-metrics.flush-delay-ms:5000}")
    public void flush() {
        try {
            metricService.flushPending();
        } catch (RuntimeException exception) {
            log.warn("业务监控事件批量落库失败，将在下个周期重试", exception);
        }
    }
}
