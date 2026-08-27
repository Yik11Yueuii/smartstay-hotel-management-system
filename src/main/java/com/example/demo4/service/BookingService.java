package com.example.demo4.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo4.entity.Booking;

public interface BookingService extends IService<Booking> {
    void createBooking(Booking booking, Long authenticatedUserId);
    void confirmBooking(Long id);
    void cancelBooking(Long id, Long authenticatedUserId, Integer authenticatedRole);
}
