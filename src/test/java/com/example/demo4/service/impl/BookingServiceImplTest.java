package com.example.demo4.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.demo4.entity.Booking;
import com.example.demo4.entity.Room;
import com.example.demo4.service.RoomService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class BookingServiceImplTest {

    @Test
    void createBookingCalculatesTrustedRoomPriceOnServer() {
        RoomService roomService = mock(RoomService.class);
        when(roomService.getById(8L)).thenReturn(availableRoom());
        BookingServiceImpl service = spy(new BookingServiceImpl(roomService));
        doReturn(0L).when(service).count(any(Wrapper.class));
        doReturn(true).when(service).save(any(Booking.class));
        Booking booking = validBooking();
        booking.setPrice(new BigDecimal("0.01"));
        booking.setTotalAmount(new BigDecimal("0.01"));
        booking.setUserId(999L);

        service.createBooking(booking, 2L);

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
        when(roomService.getById(8L)).thenReturn(availableRoom());
        BookingServiceImpl service = spy(new BookingServiceImpl(roomService));
        doReturn(1L).when(service).count(any(Wrapper.class));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.createBooking(validBooking(), 2L));

        assertEquals("所选日期内客房已被预订", exception.getMessage());
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
