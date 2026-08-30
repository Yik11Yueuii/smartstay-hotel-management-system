package com.smartstay.hotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartstay.hotel.entity.BusinessMetricEvent;
import com.smartstay.hotel.mapper.BusinessMetricEventMapper;
import com.smartstay.hotel.service.BusinessMetricService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class BusinessMetricServiceImpl extends ServiceImpl<BusinessMetricEventMapper, BusinessMetricEvent>
        implements BusinessMetricService {
    private static final int MAX_PENDING_EVENTS = 5000;
    private static final int BATCH_SIZE = 200;
    private final Queue<BusinessMetricEvent> pending = new ConcurrentLinkedQueue<>();

    @Override
    public void record(String eventType, Long roomId, Long userId, Long bookingId, String detail) {
        BusinessMetricEvent event = new BusinessMetricEvent();
        event.setEventType(eventType);
        event.setRoomId(roomId);
        event.setUserId(userId);
        event.setBookingId(bookingId);
        event.setDetail(detail);
        event.setCreateTime(LocalDateTime.now());
        while (pending.size() >= MAX_PENDING_EVENTS) pending.poll();
        pending.offer(event);
    }

    @Override
    public synchronized List<BusinessMetricEvent> recentSince(LocalDateTime since) {
        List<BusinessMetricEvent> result = new ArrayList<>(list(new LambdaQueryWrapper<BusinessMetricEvent>()
                .ge(BusinessMetricEvent::getCreateTime, since)
                .orderByAsc(BusinessMetricEvent::getCreateTime)));
        pending.stream()
                .filter(event -> event.getCreateTime() != null && !event.getCreateTime().isBefore(since))
                .forEach(result::add);
        result.sort(java.util.Comparator.comparing(BusinessMetricEvent::getCreateTime));
        return result;
    }

    @Override
    @Transactional
    public synchronized int flushPending() {
        List<BusinessMetricEvent> batch = new ArrayList<>();
        while (batch.size() < BATCH_SIZE) {
            BusinessMetricEvent event = pending.poll();
            if (event == null) break;
            batch.add(event);
        }
        if (batch.isEmpty()) return 0;
        try {
            saveBatch(batch);
            return batch.size();
        } catch (RuntimeException exception) {
            batch.forEach(pending::offer);
            throw exception;
        }
    }
}
