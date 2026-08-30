package com.example.demo4.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo4.entity.HousekeepingTask;
import com.example.demo4.operations.event.BookingCheckedOutEvent;

public interface HousekeepingService extends IService<HousekeepingTask> {
    HousekeepingTask createCheckoutTask(BookingCheckedOutEvent event);

    HousekeepingTask startTask(Long taskId, String assignee);

    HousekeepingTask completeTask(Long taskId);

    boolean hasActiveTask(Long roomId);
}
