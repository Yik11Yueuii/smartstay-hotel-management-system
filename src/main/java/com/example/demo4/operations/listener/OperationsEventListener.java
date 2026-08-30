package com.example.demo4.operations.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo4.common.BookingStatus;
import com.example.demo4.common.RoomStatus;
import com.example.demo4.entity.Booking;
import com.example.demo4.entity.Room;
import com.example.demo4.mapper.BookingMapper;
import com.example.demo4.operations.event.BookingCheckedOutEvent;
import com.example.demo4.operations.event.HousekeepingCompletedEvent;
import com.example.demo4.service.HousekeepingService;
import com.example.demo4.service.OperationReminderService;
import com.example.demo4.service.RoomService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OperationsEventListener {
    private final HousekeepingService housekeepingService;
    private final OperationReminderService reminderService;
    private final RoomService roomService;
    private final BookingMapper bookingMapper;

    public OperationsEventListener(HousekeepingService housekeepingService,
                                   OperationReminderService reminderService,
                                   RoomService roomService,
                                   BookingMapper bookingMapper) {
        this.housekeepingService = housekeepingService;
        this.reminderService = reminderService;
        this.roomService = roomService;
        this.bookingMapper = bookingMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBookingCheckedOut(BookingCheckedOutEvent event) {
        housekeepingService.createCheckoutTask(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onHousekeepingCompleted(HousekeepingCompletedEvent event) {
        reminderService.resolveByTask(event.taskId(), java.time.LocalDateTime.now());
        if (housekeepingService.hasActiveTask(event.roomId())) return;

        Room room = roomService.getById(event.roomId());
        if (room == null || Integer.valueOf(RoomStatus.MAINTENANCE.getCode()).equals(room.getStatus())) return;

        long checkedIn = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getRoomId, event.roomId())
                .eq(Booking::getStatus, BookingStatus.CHECKED_IN.getCode()));
        long confirmed = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getRoomId, event.roomId())
                .eq(Booking::getStatus, BookingStatus.CONFIRMED.getCode()));
        room.setStatus(checkedIn > 0 ? RoomStatus.OCCUPIED.getCode()
                : confirmed > 0 ? RoomStatus.RESERVED.getCode() : RoomStatus.AVAILABLE.getCode());
        roomService.updateById(room);
    }
}
