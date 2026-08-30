package com.example.demo4.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo4.entity.Booking;
import com.example.demo4.common.BookingStatus;
import com.example.demo4.entity.Room;
import com.example.demo4.mapper.BookingMapper;
import com.example.demo4.mapper.RoomMapper;
import com.example.demo4.service.BookingService;
import com.example.demo4.service.RoomService;
import com.example.demo4.exception.BusinessException;
import com.example.demo4.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    public BookingServiceImpl(RoomService roomService, RoomMapper roomMapper) {
        this.roomService = roomService;
        this.roomMapper = roomMapper;
    }

    @Override
    @Transactional
    public Booking createBooking(Booking booking, Long authenticatedUserId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        if (booking == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "预订信息不能为空");
        }

        Booking existing = findIdempotentBooking(authenticatedUserId, idempotencyKey);
        if (existing != null) return requireSameRequest(existing, booking);
        validateBookingRequest(booking);

        Room room = roomMapper.selectByIdForUpdate(booking.getRoomId());
        if (room == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "客房不存在");
        }
        if (room.getStatus() == null || room.getStatus() == 3 || room.getStatus() == 4) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "客房当前不可预订");
        }

        existing = findIdempotentBooking(authenticatedUserId, idempotencyKey);
        if (existing != null) return requireSameRequest(existing, booking);

        LambdaQueryWrapper<Booking> conflict = new LambdaQueryWrapper<>();
        conflict.eq(Booking::getRoomId, room.getId())
                .in(Booking::getStatus, BookingStatus.PENDING.getCode(),
                        BookingStatus.CONFIRMED.getCode(), BookingStatus.CHECKED_IN.getCode())
                .lt(Booking::getCheckInDate, booking.getCheckOutDate())
                .gt(Booking::getCheckOutDate, booking.getCheckInDate());
        if (this.count(conflict) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "所选日期内客房已被预订");
        }

        long days = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        BigDecimal unitPrice = room.getIsPromotion() != null && room.getIsPromotion() == 1
                && room.getPromotionPrice() != null ? room.getPromotionPrice() : room.getPrice();

        booking.setOrderNo("ORD" + System.currentTimeMillis()
                + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase());
        booking.setUserId(authenticatedUserId);
        booking.setIdempotencyKey(idempotencyKey);
        booking.setRoomName(room.getRoomName());
        booking.setRoomNumber(room.getRoomNumber());
        booking.setDays(Math.toIntExact(days));
        booking.setPrice(unitPrice);
        booking.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(days)));
        booking.setStatus(BookingStatus.PENDING.getCode());
        if (!this.save(booking)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "预订创建失败");
        }
        return booking;
    }

    private void validateIdempotencyKey(String key) {
        if (StrUtil.isBlank(key) || key.length() < 16 || key.length() > 64
                || !key.matches("^[A-Za-z0-9_-]+$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Idempotency-Key 必须为16-64位字母、数字、下划线或短横线");
        }
    }

    private Booking findIdempotentBooking(Long userId, String key) {
        return this.getOne(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getUserId, userId)
                .eq(Booking::getIdempotencyKey, key), false);
    }

    private Booking requireSameRequest(Booking existing, Booking requested) {
        if (!existing.getRoomId().equals(requested.getRoomId())
                || !existing.getCheckInDate().equals(requested.getCheckInDate())
                || !existing.getCheckOutDate().equals(requested.getCheckOutDate())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "同一 Idempotency-Key 不能用于不同预订请求");
        }
        return existing;
    }

    @Override
    @Transactional
    public void confirmBooking(Long id) {
        Booking booking = this.getById(id);
        if (booking == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        BookingStatus.fromCode(booking.getStatus()).requireTransitionTo(BookingStatus.CONFIRMED);
        Room room = requireRoom(booking.getRoomId());
        if (room.getStatus() == 4) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "维护中的客房不能确认预订");
        }
        booking.setStatus(BookingStatus.CONFIRMED.getCode());
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
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        if (authenticatedRole != 1 && !authenticatedUserId.equals(booking.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能取消其他用户的订单");
        }
        BookingStatus.fromCode(booking.getStatus()).requireTransitionTo(BookingStatus.CANCELLED);
        booking.setStatus(BookingStatus.CANCELLED.getCode());
        this.updateById(booking);
        refreshRoomStatus(booking.getRoomId());
    }

    @Override
    @Transactional
    public void checkIn(Long id, String guestName, String guestIdCard, BigDecimal deposit) {
        Booking booking = requireBooking(id);
        BookingStatus.fromCode(booking.getStatus()).requireTransitionTo(BookingStatus.CHECKED_IN);
        if (StrUtil.isBlank(guestName) || StrUtil.isBlank(guestIdCard)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "入住人姓名和证件号不能为空");
        }
        if (deposit == null || deposit.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "押金不能为负数");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(booking.getCheckInDate()) || !today.isBefore(booking.getCheckOutDate())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "当前日期不在订单入住期间");
        }
        Room room = requireRoom(booking.getRoomId());
        if (room.getStatus() == 4) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "维护中的客房不能办理入住");
        }
        if (room.getStatus() == 3) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "客房当前已有住客");
        }

        booking.setGuestName(guestName);
        booking.setGuestIdCard(guestIdCard);
        booking.setDeposit(deposit);
        booking.setActualCheckInTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.CHECKED_IN.getCode());
        this.updateById(booking);

        room.setStatus(3);
        roomService.updateById(room);
    }

    @Override
    @Transactional
    public void checkOut(Long id, BigDecimal depositReturn, BigDecimal additionalCharges, String remark) {
        Booking booking = requireBooking(id);
        BookingStatus.fromCode(booking.getStatus()).requireTransitionTo(BookingStatus.CHECKED_OUT);
        if (depositReturn == null || depositReturn.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "退还押金不能为负数");
        }
        BigDecimal deposit = booking.getDeposit() == null ? BigDecimal.ZERO : booking.getDeposit();
        if (depositReturn.compareTo(deposit) > 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "退还押金不能超过已收押金");
        }
        if (additionalCharges == null || additionalCharges.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "额外费用不能为负数");
        }

        booking.setDepositReturn(depositReturn);
        booking.setAdditionalCharges(additionalCharges);
        booking.setRemark(remark == null ? "" : remark);
        booking.setActualCheckOutTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.CHECKED_OUT.getCode());
        this.updateById(booking);
        refreshRoomStatus(booking.getRoomId());
    }

    private void validateBookingRequest(Booking booking) {
        if (booking.getRoomId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "请选择客房");
        }
        LocalDate checkIn = booking.getCheckInDate();
        LocalDate checkOut = booking.getCheckOutDate();
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "退房日期必须晚于入住日期");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "入住日期不能早于今天");
        }
        if (StrUtil.isBlank(booking.getContactName()) || StrUtil.isBlank(booking.getContactPhone())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "联系人和联系电话不能为空");
        }
    }

    private Booking requireBooking(Long id) {
        Booking booking = this.getById(id);
        if (booking == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        return booking;
    }

    private Room requireRoom(Long roomId) {
        Room room = roomService.getById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "客房不存在");
        }
        return room;
    }

    private void refreshRoomStatus(Long roomId) {
        Room room = requireRoom(roomId);
        if (room.getStatus() == 4) {
            return;
        }
        LambdaQueryWrapper<Booking> checkedIn = new LambdaQueryWrapper<>();
        checkedIn.eq(Booking::getRoomId, roomId).eq(Booking::getStatus, BookingStatus.CHECKED_IN.getCode());
        LambdaQueryWrapper<Booking> confirmed = new LambdaQueryWrapper<>();
        confirmed.eq(Booking::getRoomId, roomId).eq(Booking::getStatus, BookingStatus.CONFIRMED.getCode());
        room.setStatus(this.count(checkedIn) > 0 ? 3 : this.count(confirmed) > 0 ? 2 : 1);
        roomService.updateById(room);
    }
}
