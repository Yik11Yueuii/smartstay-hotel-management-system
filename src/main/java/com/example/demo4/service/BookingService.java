package com.example.demo4.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo4.entity.Booking;

import java.math.BigDecimal;

public interface BookingService extends IService<Booking> {
    Booking createBooking(Booking booking, Long authenticatedUserId, String idempotencyKey);
    void confirmBooking(Long id);
    void cancelBooking(Long id, Long authenticatedUserId, Integer authenticatedRole);
    void checkIn(Long id, String guestName, String guestIdCard, BigDecimal deposit);
    void checkOut(Long id, BigDecimal depositReturn, BigDecimal additionalCharges, String remark);
}
