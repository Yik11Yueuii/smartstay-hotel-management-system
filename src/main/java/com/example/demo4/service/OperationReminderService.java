package com.example.demo4.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo4.entity.OperationReminder;

import java.time.LocalDateTime;

public interface OperationReminderService extends IService<OperationReminder> {
    int scanAndCreate(LocalDateTime now);

    void resolve(Long reminderId);

    void resolveByTask(Long taskId, LocalDateTime resolvedTime);
}
