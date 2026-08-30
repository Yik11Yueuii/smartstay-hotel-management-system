package com.smartstay.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartstay.hotel.entity.BusinessMetricEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface BusinessMetricService extends IService<BusinessMetricEvent> {
    void record(String eventType, Long roomId, Long userId, Long bookingId, String detail);

    List<BusinessMetricEvent> recentSince(LocalDateTime since);

    int flushPending();
}
