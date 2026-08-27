package com.example.demo4.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo4.entity.Booking;
import com.example.demo4.mapper.BookingMapper;
import com.example.demo4.service.BookingService;
import org.springframework.stereotype.Service;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    @Override
    public void createBooking(Booking booking) {
        String orderNo = "ORD" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss");
        booking.setOrderNo(orderNo);
        booking.setStatus(0);
        this.save(booking);
    }

    @Override
    public void confirmBooking(Long id) {
        Booking booking = this.getById(id);
        if (booking == null) {
            throw new RuntimeException("订单不存在");
        }
        booking.setStatus(1);
        this.updateById(booking);
    }

    @Override
    public void cancelBooking(Long id) {
        Booking booking = this.getById(id);
        if (booking == null) {
            throw new RuntimeException("订单不存在");
        }
        booking.setStatus(2);
        this.updateById(booking);
    }
}