package com.example.demo4.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo4.common.Result;
import com.example.demo4.entity.Booking;
import com.example.demo4.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/booking")
@CrossOrigin
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * 分页查询订单列表
     */
    @GetMapping("/list")
    public Result<Page<Booking>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId) {

        Page<Booking> pageInfo = new Page<>(page, size);
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();

        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(Booking::getOrderNo, orderNo);
        }

        if (status != null) {
            wrapper.eq(Booking::getStatus, status);
        }

        if (userId != null) {
            wrapper.eq(Booking::getUserId, userId);
        }

        wrapper.orderByDesc(Booking::getCreateTime);
        Page<Booking> result = bookingService.page(pageInfo, wrapper);

        return Result.success(result);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<Booking> getById(@PathVariable Long id) {
        Booking booking = bookingService.getById(id);
        return Result.success(booking);
    }

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<String> create(@RequestBody Booking booking) {
        try {
            bookingService.createBooking(booking);
            return Result.success("预订成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 确认订单
     */
    @PutMapping("/confirm/{id}")
    public Result<String> confirm(@PathVariable Long id) {
        try {
            bookingService.confirmBooking(id);
            return Result.success("确认成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PutMapping("/cancel/{id}")
    public Result<String> cancel(@PathVariable Long id) {
        try {
            bookingService.cancelBooking(id);
            return Result.success("取消成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 办理入住
     */
    @PostMapping("/checkin")
    public Result<String> checkIn(@RequestBody Map<String, Object> params) {
        try {
            Long bookingId = Long.valueOf(params.get("bookingId").toString());
            String guestName = params.get("guestName").toString();
            String guestIdCard = params.get("guestIdCard").toString();
            Double deposit = Double.valueOf(params.get("deposit").toString());

            Booking booking = bookingService.getById(bookingId);
            if (booking == null) {
                return Result.error("订单不存在");
            }

            booking.setGuestName(guestName);
            booking.setGuestIdCard(guestIdCard);
            booking.setDeposit(BigDecimal.valueOf(deposit));
            booking.setActualCheckInTime(LocalDateTime.now());
            booking.setStatus(3);

            bookingService.updateById(booking);
            return Result.success("入住成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 办理退房
     */
    @PostMapping("/checkout")
    public Result<String> checkOut(@RequestBody Map<String, Object> params) {
        try {
            Long bookingId = Long.valueOf(params.get("bookingId").toString());
            Double depositReturn = Double.valueOf(params.get("depositReturn").toString());
            Double additionalCharges = Double.valueOf(params.get("additionalCharges").toString());
            String remark = params.get("remark") != null ? params.get("remark").toString() : "";

            Booking booking = bookingService.getById(bookingId);
            if (booking == null) {
                return Result.error("订单不存在");
            }

            booking.setDepositReturn(BigDecimal.valueOf(depositReturn));
            booking.setAdditionalCharges(BigDecimal.valueOf(additionalCharges));
            booking.setRemark(remark);
            booking.setActualCheckOutTime(LocalDateTime.now());
            booking.setStatus(4);

            bookingService.updateById(booking);
            return Result.success("退房成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}