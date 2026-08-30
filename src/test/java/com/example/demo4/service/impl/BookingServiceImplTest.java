package com.example.demo4.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.demo4.entity.Booking;
import com.example.demo4.entity.Room;
import com.example.demo4.service.RoomService;
import com.example.demo4.mapper.RoomMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class BookingServiceImplTest {

    @Test
    void createBookingCalculatesTrustedRoomPriceOnServer() {
        RoomService roomService = mock(RoomService.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        when(roomMapper.selectByIdForUpdate(8L)).thenReturn(availableRoom());
        BookingServiceImpl service = spy(new BookingServiceImpl(roomService, roomMapper));
        doReturn(null).when(service).getOne(any(Wrapper.class), eq(false));
        doReturn(0L).when(service).count(any(Wrapper.class));
        doReturn(true).when(service).save(any(Booking.class));
        Booking booking = validBooking();
        booking.setPrice(new BigDecimal("0.01"));
        booking.setTotalAmount(new BigDecimal("0.01"));
        booking.setUserId(999L);

        service.createBooking(booking, 2L, "test-key-0000000001");

        assertEquals(2L, booking.getUserId());
        assertEquals("豪华双人间", booking.getRoomName());
        assertEquals(3, booking.getDays());
        assertEquals(new BigDecimal("388.00"), booking.getPrice());
        assertEquals(new BigDecimal("1164.00"), booking.getTotalAmount());
        assertEquals(0, booking.getStatus());
    }

    @Test
    void createBookingRejectsOverlappingReservation() {
        RoomService roomService = mock(RoomService.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        when(roomMapper.selectByIdForUpdate(8L)).thenReturn(availableRoom());
        BookingServiceImpl service = spy(new BookingServiceImpl(roomService, roomMapper));
        doReturn(null).when(service).getOne(any(Wrapper.class), eq(false));
        doReturn(1L).when(service).count(any(Wrapper.class));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.createBooking(validBooking(), 2L, "test-key-0000000002"));

        assertEquals("所选日期内客房已被预订", exception.getMessage());
    }

    @Test
    void checkInMovesConfirmedBookingAndRoomToCheckedIn() {
        RoomService roomService = mock(RoomService.class);
        Room room = availableRoom();
        room.setStatus(2);
        when(roomService.getById(8L)).thenReturn(room);
        Booking booking = validBooking();
        booking.setId(10L);
        booking.setStatus(1);
        booking.setCheckInDate(LocalDate.now());
        booking.setCheckOutDate(LocalDate.now().plusDays(1));
        BookingServiceImpl service = spy(new BookingServiceImpl(roomService, mock(RoomMapper.class)));
        doReturn(booking).when(service).getById(10L);
        doReturn(true).when(service).updateById(any(Booking.class));

        service.checkIn(10L, "张三", "110101199001011234", new BigDecimal("300.00"));

        assertEquals(3, booking.getStatus());
        assertEquals(3, room.getStatus());
        assertEquals(new BigDecimal("300.00"), booking.getDeposit());
    }

    @Test
    void checkOutRejectsDepositReturnAboveCollectedDeposit() {
        RoomService roomService = mock(RoomService.class);
        Booking booking = validBooking();
        booking.setStatus(3);
        booking.setDeposit(new BigDecimal("200.00"));
        BookingServiceImpl service = spy(new BookingServiceImpl(roomService, mock(RoomMapper.class)));
        doReturn(booking).when(service).getById(10L);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.checkOut(10L, new BigDecimal("201.00"), BigDecimal.ZERO, ""));

        assertEquals("退还押金不能超过已收押金", exception.getMessage());
    }

    @Test
    void confirmRejectsBookingThatIsNotPending() {
        Booking booking = validBooking();
        booking.setStatus(2);
        BookingServiceImpl service = spy(new BookingServiceImpl(mock(RoomService.class), mock(RoomMapper.class)));
        doReturn(booking).when(service).getById(10L);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.confirmBooking(10L));

        assertEquals("订单不能从“已取消”变更为“已确认”", exception.getMessage());
    }

    private Booking validBooking() {
        Booking booking = new Booking();
        booking.setRoomId(8L);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(13));
        booking.setContactName("测试用户");
        booking.setContactPhone("13800000000");
        return booking;
    }

    private Room availableRoom() {
        Room room = new Room();
        room.setId(8L);
        room.setRoomName("豪华双人间");
        room.setRoomNumber("0808");
        room.setStatus(1);
        room.setIsPromotion(0);
        room.setPrice(new BigDecimal("388.00"));
        return room;
    }
}
