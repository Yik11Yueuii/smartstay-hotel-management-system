package com.smartstay.hotel.operations.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartstay.hotel.common.BookingStatus;
import com.smartstay.hotel.common.RoomStatus;
import com.smartstay.hotel.entity.Booking;
import com.smartstay.hotel.entity.Room;
import com.smartstay.hotel.mapper.BookingMapper;
import com.smartstay.hotel.operations.event.BookingCheckedOutEvent;
import com.smartstay.hotel.operations.event.HousekeepingCompletedEvent;
import com.smartstay.hotel.service.HousekeepingService;
import com.smartstay.hotel.service.OperationReminderService;
import com.smartstay.hotel.service.RoomService;
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
