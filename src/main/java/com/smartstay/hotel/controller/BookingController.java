package com.smartstay.hotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartstay.hotel.common.Result;
import com.smartstay.hotel.config.JwtAuthenticationFilter;
import com.smartstay.hotel.entity.Booking;
import com.smartstay.hotel.service.BookingService;
import com.smartstay.hotel.exception.BusinessException;
import com.smartstay.hotel.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long authenticatedUserId,
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ROLE) Integer authenticatedRole,
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

        if (authenticatedRole != 1) {
            wrapper.eq(Booking::getUserId, authenticatedUserId);
        } else if (userId != null) {
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
    public Result<Booking> getById(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long authenticatedUserId,
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ROLE) Integer authenticatedRole,
            @PathVariable Long id) {
        Booking booking = bookingService.getById(id);
        if (booking == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        if (authenticatedRole != 1
                && !authenticatedUserId.equals(booking.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能查看其他用户的订单");
        }
        return Result.success(booking);
    }

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<Booking> create(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long authenticatedUserId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Booking booking) {
        return Result.success(bookingService.createBooking(booking, authenticatedUserId, idempotencyKey));
    }

    /**
     * 确认订单
     */
    @PutMapping("/confirm/{id}")
    public Result<String> confirm(@PathVariable Long id) {
        bookingService.confirmBooking(id);
        return Result.success("确认成功");
    }

    /**
     * 取消订单
     */
    @PutMapping("/cancel/{id}")
    public Result<String> cancel(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long authenticatedUserId,
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ROLE) Integer authenticatedRole,
            @PathVariable Long id) {
        bookingService.cancelBooking(id, authenticatedUserId, authenticatedRole);
        return Result.success("取消成功");
    }

    /**
     * 办理入住
     */
    @PostMapping("/checkin")
    public Result<String> checkIn(@RequestBody Map<String, Object> params) {
        Long bookingId = Long.valueOf(required(params, "bookingId"));
        String guestName = required(params, "guestName");
        String guestIdCard = required(params, "guestIdCard");
        BigDecimal deposit = new BigDecimal(required(params, "deposit"));
        bookingService.checkIn(bookingId, guestName, guestIdCard, deposit);
        return Result.success("入住成功");
    }

    /**
     * 办理退房
     */
    @PostMapping("/checkout")
    public Result<String> checkOut(@RequestBody Map<String, Object> params) {
        Long bookingId = Long.valueOf(required(params, "bookingId"));
        BigDecimal depositReturn = new BigDecimal(required(params, "depositReturn"));
        BigDecimal additionalCharges = new BigDecimal(required(params, "additionalCharges"));
        String remark = params.get("remark") != null ? params.get("remark").toString() : "";
        bookingService.checkOut(bookingId, depositReturn, additionalCharges, remark);
        return Result.success("退房成功");
    }

    private String required(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "缺少必填参数: " + key);
        }
        return value.toString();
    }
}
