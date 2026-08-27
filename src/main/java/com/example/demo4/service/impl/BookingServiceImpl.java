package com.example.demo4.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo4.entity.Booking;
import com.example.demo4.entity.Room;
import com.example.demo4.mapper.BookingMapper;
import com.example.demo4.service.BookingService;
import com.example.demo4.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private final RoomService roomService;

    public BookingServiceImpl(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    @Transactional
    public void createBooking(Booking booking, Long authenticatedUserId) {
        validateBookingRequest(booking);

        Room room = roomService.getById(booking.getRoomId());
        if (room == null) {
            throw new RuntimeException("客房不存在");
        }
        if (room.getStatus() == null || room.getStatus() == 3 || room.getStatus() == 4) {
            throw new RuntimeException("客房当前不可预订");
        }

        LambdaQueryWrapper<Booking> conflict = new LambdaQueryWrapper<>();
        conflict.eq(Booking::getRoomId, room.getId())
                .in(Booking::getStatus, 0, 1, 3)
                .lt(Booking::getCheckInDate, booking.getCheckOutDate())
                .gt(Booking::getCheckOutDate, booking.getCheckInDate());
        if (this.count(conflict) > 0) {
            throw new RuntimeException("所选日期内客房已被预订");
        }

        long days = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        BigDecimal unitPrice = room.getIsPromotion() != null && room.getIsPromotion() == 1
                && room.getPromotionPrice() != null ? room.getPromotionPrice() : room.getPrice();

        booking.setOrderNo("ORD" + System.currentTimeMillis()
                + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase());
        booking.setUserId(authenticatedUserId);
        booking.setRoomName(room.getRoomName());
        booking.setRoomNumber(room.getRoomNumber());
        booking.setDays(Math.toIntExact(days));
        booking.setPrice(unitPrice);
        booking.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(days)));
        booking.setStatus(0);
        if (!this.save(booking)) {
            throw new RuntimeException("预订创建失败");
        }
    }

    @Override
    @Transactional
    public void confirmBooking(Long id) {
        Booking booking = this.getById(id);
        if (booking == null) {
            throw new RuntimeException("订单不存在");
        }
        if (booking.getStatus() != 0) {
            throw new RuntimeException("只有待确认订单可以确认");
        }
        Room room = requireRoom(booking.getRoomId());
        if (room.getStatus() == 4) {
            throw new RuntimeException("维护中的客房不能确认预订");
        }
        booking.setStatus(1);
        this.updateById(booking);
        if (room.getStatus() == 1) {
            room.setStatus(2);
            roomService.updateById(room);
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long id, Long authenticatedUserId, Integer authenticatedRole) {
        Booking booking = this.getById(id);
        if (booking == null) {
            throw new RuntimeException("订单不存在");
        }
        if (authenticatedRole != 1 && !authenticatedUserId.equals(booking.getUserId())) {
            throw new RuntimeException("不能取消其他用户的订单");
        }
        if (booking.getStatus() != 0 && booking.getStatus() != 1) {
            throw new RuntimeException("当前订单状态不能取消");
        }
        booking.setStatus(2);
        this.updateById(booking);
        refreshRoomStatus(booking.getRoomId());
    }

    @Override
    @Transactional
    public void checkIn(Long id, String guestName, String guestIdCard, BigDecimal deposit) {
        Booking booking = requireBooking(id);
        if (booking.getStatus() != 1) {
            throw new RuntimeException("只有已确认订单可以办理入住");
        }
        if (StrUtil.isBlank(guestName) || StrUtil.isBlank(guestIdCard)) {
            throw new RuntimeException("入住人姓名和证件号不能为空");
        }
        if (deposit == null || deposit.signum() < 0) {
            throw new RuntimeException("押金不能为负数");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(booking.getCheckInDate()) || !today.isBefore(booking.getCheckOutDate())) {
            throw new RuntimeException("当前日期不在订单入住期间");
        }
        Room room = requireRoom(booking.getRoomId());
        if (room.getStatus() == 4) {
            throw new RuntimeException("维护中的客房不能办理入住");
        }
        if (room.getStatus() == 3) {
            throw new RuntimeException("客房当前已有住客");
        }

        booking.setGuestName(guestName);
        booking.setGuestIdCard(guestIdCard);
        booking.setDeposit(deposit);
        booking.setActualCheckInTime(LocalDateTime.now());
        booking.setStatus(3);
        this.updateById(booking);

        room.setStatus(3);
        roomService.updateById(room);
    }

    @Override
    @Transactional
    public void checkOut(Long id, BigDecimal depositReturn, BigDecimal additionalCharges, String remark) {
        Booking booking = requireBooking(id);
        if (booking.getStatus() != 3) {
            throw new RuntimeException("只有已入住订单可以办理退房");
        }
        if (depositReturn == null || depositReturn.signum() < 0) {
            throw new RuntimeException("退还押金不能为负数");
        }
        BigDecimal deposit = booking.getDeposit() == null ? BigDecimal.ZERO : booking.getDeposit();
        if (depositReturn.compareTo(deposit) > 0) {
            throw new RuntimeException("退还押金不能超过已收押金");
        }
        if (additionalCharges == null || additionalCharges.signum() < 0) {
            throw new RuntimeException("额外费用不能为负数");
        }

        booking.setDepositReturn(depositReturn);
        booking.setAdditionalCharges(additionalCharges);
        booking.setRemark(remark == null ? "" : remark);
        booking.setActualCheckOutTime(LocalDateTime.now());
        booking.setStatus(4);
        this.updateById(booking);
        refreshRoomStatus(booking.getRoomId());
    }

    private void validateBookingRequest(Booking booking) {
        if (booking.getRoomId() == null) {
            throw new RuntimeException("请选择客房");
        }
        LocalDate checkIn = booking.getCheckInDate();
        LocalDate checkOut = booking.getCheckOutDate();
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new RuntimeException("退房日期必须晚于入住日期");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new RuntimeException("入住日期不能早于今天");
        }
        if (StrUtil.isBlank(booking.getContactName()) || StrUtil.isBlank(booking.getContactPhone())) {
            throw new RuntimeException("联系人和联系电话不能为空");
        }
    }

    private Booking requireBooking(Long id) {
        Booking booking = this.getById(id);
        if (booking == null) {
            throw new RuntimeException("订单不存在");
        }
        return booking;
    }

    private Room requireRoom(Long roomId) {
        Room room = roomService.getById(roomId);
        if (room == null) {
            throw new RuntimeException("客房不存在");
        }
        return room;
    }

    private void refreshRoomStatus(Long roomId) {
        Room room = requireRoom(roomId);
        if (room.getStatus() == 4) {
            return;
        }
        LambdaQueryWrapper<Booking> checkedIn = new LambdaQueryWrapper<>();
        checkedIn.eq(Booking::getRoomId, roomId).eq(Booking::getStatus, 3);
        LambdaQueryWrapper<Booking> confirmed = new LambdaQueryWrapper<>();
        confirmed.eq(Booking::getRoomId, roomId).eq(Booking::getStatus, 1);
        room.setStatus(this.count(checkedIn) > 0 ? 3 : this.count(confirmed) > 0 ? 2 : 1);
        roomService.updateById(room);
    }
}
