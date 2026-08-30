package com.smartstay.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartstay.hotel.entity.HousekeepingTask;
import com.smartstay.hotel.operations.event.BookingCheckedOutEvent;

public interface HousekeepingService extends IService<HousekeepingTask> {
    HousekeepingTask createCheckoutTask(BookingCheckedOutEvent event);

    HousekeepingTask startTask(Long taskId, String assignee);

    HousekeepingTask completeTask(Long taskId);

    boolean hasActiveTask(Long roomId);
}
