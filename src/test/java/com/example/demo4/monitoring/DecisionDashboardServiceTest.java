package com.example.demo4.monitoring;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.demo4.common.BusinessMetricType;
import com.example.demo4.entity.Booking;
import com.example.demo4.entity.BusinessMetricEvent;
import com.example.demo4.entity.Room;
import com.example.demo4.service.BookingService;
import com.example.demo4.service.BusinessMetricService;
import com.example.demo4.service.HousekeepingService;
import com.example.demo4.service.OperationReminderService;
import com.example.demo4.service.RoomService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DecisionDashboardServiceTest {
    @Test
    void snapshotConnectsOccupancyPricingProtectionAndOperationsToDecisionMetrics() {
        BookingService bookingService = mock(BookingService.class);
        RoomService roomService = mock(RoomService.class);
        BusinessMetricService metricService = mock(BusinessMetricService.class);
        HousekeepingService housekeepingService = mock(HousekeepingService.class);
        OperationReminderService reminderService = mock(OperationReminderService.class);
        ApiPerformanceMonitor performanceMonitor = mock(ApiPerformanceMonitor.class);

        when(roomService.list()).thenReturn(List.of(room(1L, 1), room(2L, 1), room(3L, 4)));
        Booking forecast = booking(1L, LocalDate.now(), LocalDate.now().plusDays(2));
        Booking priced = booking(1L, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        priced.setCreateTime(LocalDateTime.now().minusDays(1));
        priced.setBasePrice(new BigDecimal("100.00"));
        priced.setDays(2);
        priced.setTotalAmount(new BigDecimal("220.00"));
        priced.setPricingStrategyVersion("SMART_PRICING_V1");
        when(bookingService.list(any(Wrapper.class))).thenReturn(List.of(forecast), List.of(priced));
        when(metricService.recentSince(any(LocalDateTime.class))).thenReturn(List.of(
                event(BusinessMetricType.INVENTORY_CONFLICT),
                event(BusinessMetricType.IDEMPOTENT_REPLAY)));
        when(housekeepingService.count(any(Wrapper.class))).thenReturn(1L, 0L, 1L);
        when(reminderService.count(any(Wrapper.class))).thenReturn(1L);
        when(performanceMonitor.snapshot()).thenReturn(Map.of(
                "health", "HEALTHY", "p95Ms", 120L, "errorRate", 0.0));

        DecisionDashboardService service = new DecisionDashboardService(
                bookingService, roomService, metricService, housekeepingService, reminderService, performanceMonitor);
        Map<String, Object> snapshot = service.snapshot();

        @SuppressWarnings("unchecked")
        Map<String, Object> occupancy = (Map<String, Object>) snapshot.get("occupancy");
        @SuppressWarnings("unchecked")
        Map<String, Object> pricing = (Map<String, Object>) snapshot.get("pricing");
        @SuppressWarnings("unchecked")
        Map<String, Object> protection = (Map<String, Object>) snapshot.get("protection");
        @SuppressWarnings("unchecked")
        Map<String, Object> operations = (Map<String, Object>) snapshot.get("operations");
        assertEquals(50.0, occupancy.get("todayRate"));
        assertEquals(new BigDecimal("20.00"), pricing.get("netImpact"));
        assertEquals(100.0, pricing.get("coverageRate"));
        assertEquals(2L, protection.get("protectedRequests7d"));
        assertEquals(1L, operations.get("overdueTasks"));
    }

    private Room room(Long id, int status) {
        Room room = new Room();
        room.setId(id);
        room.setStatus(status);
        return room;
    }

    private Booking booking(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Booking booking = new Booking();
        booking.setRoomId(roomId);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setStatus(1);
        booking.setCreateTime(LocalDateTime.now());
        return booking;
    }

    private BusinessMetricEvent event(String type) {
        BusinessMetricEvent event = new BusinessMetricEvent();
        event.setEventType(type);
        event.setCreateTime(LocalDateTime.now());
        return event;
    }
}
