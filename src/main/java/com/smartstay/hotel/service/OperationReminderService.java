package com.smartstay.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartstay.hotel.entity.OperationReminder;

import java.time.LocalDateTime;

public interface OperationReminderService extends IService<OperationReminder> {
    int scanAndCreate(LocalDateTime now);

    void resolve(Long reminderId);

    void resolveByTask(Long taskId, LocalDateTime resolvedTime);
}
