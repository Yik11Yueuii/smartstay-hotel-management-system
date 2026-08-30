package com.example.demo4.operations.scheduler;

import com.example.demo4.service.OperationReminderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "operations.reminder.enabled", havingValue = "true", matchIfMissing = true)
public class OperationReminderScheduler {
    private final OperationReminderService reminderService;

    public OperationReminderScheduler(OperationReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(initialDelayString = "${operations.reminder.initial-delay-ms:30000}",
            fixedDelayString = "${operations.reminder.scan-delay-ms:60000}")
    public void scan() {
        reminderService.scanAndCreate(LocalDateTime.now());
    }
}
