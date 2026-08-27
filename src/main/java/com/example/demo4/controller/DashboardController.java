package com.example.demo4.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo4.common.Result;
import com.example.demo4.config.AdminOnly;
import com.example.demo4.entity.Booking;
import com.example.demo4.entity.Room;
import com.example.demo4.entity.User;
import com.example.demo4.service.BookingService;
import com.example.demo4.service.RoomService;
import com.example.demo4.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final BookingService bookingService;
    private final RoomService roomService;
    private final UserService userService;

    public DashboardController(BookingService bookingService,
                               RoomService roomService,
                               UserService userService) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.userService = userService;
    }

    @GetMapping("/summary")
    @AdminOnly
    public Result<Map<String, Object>> summary() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalBookings", bookingService.count());
        result.put("availableRooms", roomService.count(
                new LambdaQueryWrapper<Room>().eq(Room::getStatus, 1)));
        result.put("totalUsers", userService.count(
                new LambdaQueryWrapper<User>().eq(User::getRole, 0)));
        result.put("todayRevenue", calculateTodayRevenue());
        result.put("recentBookings", recentBookings());
        return Result.success(result);
    }

    private BigDecimal calculateTodayRevenue() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Booking> checkedOutToday = bookingService.list(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getStatus, 4)
                        .ge(Booking::getActualCheckOutTime, start)
                        .lt(Booking::getActualCheckOutTime, end));
        return checkedOutToday.stream()
                .map(booking -> value(booking.getTotalAmount()).add(value(booking.getAdditionalCharges())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> recentBookings() {
        Page<Booking> page = bookingService.page(
                new Page<>(1, 5),
                new LambdaQueryWrapper<Booking>().orderByDesc(Booking::getCreateTime));
        return page.getRecords().stream().map(booking -> {
            Map<String, Object> item = new HashMap<>();
            item.put("orderNo", booking.getOrderNo());
            item.put("roomName", booking.getRoomName());
            item.put("userName", booking.getContactName());
            item.put("checkInDate", booking.getCheckInDate());
            item.put("status", booking.getStatus());
            item.put("totalAmount", booking.getTotalAmount());
            return item;
        }).collect(Collectors.toList());
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
