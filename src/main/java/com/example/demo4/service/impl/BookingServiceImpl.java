package com.example.demo4.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo4.entity.Booking;
import com.example.demo4.common.BusinessMetricType;
import com.example.demo4.common.BookingStatus;
import com.example.demo4.common.RoomStatus;
import com.example.demo4.entity.Room;
import com.example.demo4.mapper.BookingMapper;
import com.example.demo4.mapper.RoomMapper;
import com.example.demo4.service.BookingService;
import com.example.demo4.service.BusinessMetricService;
import com.example.demo4.service.RoomService;
import com.example.demo4.exception.BusinessException;
import com.example.demo4.exception.ErrorCode;
import com.example.demo4.pricing.PricingQuote;
import com.example.demo4.pricing.PricingService;
import com.example.demo4.operations.event.BookingCheckedOutEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final RoomService roomService;
    private final RoomMapper roomMapper;
    private final PricingService pricingService;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessMetricService businessMetricService;

    public BookingServiceImpl(RoomService roomService, RoomMapper roomMapper, PricingService pricingService,
                              ApplicationEventPublisher eventPublisher,
                              BusinessMetricService businessMetricService) {
        this.roomService = roomService;
        this.roomMapper = roomMapper;
        this.pricingService = pricingService;
        this.eventPublisher = eventPublisher;
        this.businessMetricService = businessMetricService;
    }

    @Override
    @Transactional
    public Booking createBooking(Booking booking, Long authenticatedUserId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        if (booking == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "预订信息不能为空");
        }

        Booking existing = findIdempotentBooking(authenticatedUserId, idempotencyKey);
        if (existing != null) return replayExisting(existing, booking, authenticatedUserId);
        validateBookingRequest(booking);

        Room room = roomMapper.selectByIdForUpdate(booking.getRoomId());
        if (room == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "客房不存在");
        }
        if (room.getStatus() == null
                || room.getStatus() == RoomStatus.OCCUPIED.getCode()
                || room.getStatus() == RoomStatus.MAINTENANCE.getCode()
                || room.getStatus() == RoomStatus.CLEANING.getCode()) {
            recordMetric(BusinessMetricType.INVENTORY_CONFLICT, room.getId(), authenticatedUserId, null,
                    "不可售房态请求已拦截");
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "客房当前不可预订");
        }

        existing = findIdempotentBooking(authenticatedUserId, idempotencyKey);
        if (existing != null) return replayExisting(existing, booking, authenticatedUserId);

        LambdaQueryWrapper<Booking> conflict = new LambdaQueryWrapper<>();
        conflict.eq(Booking::getRoomId, room.getId())
                .in(Booking::getStatus, BookingStatus.PENDING.getCode(),
                        BookingStatus.CONFIRMED.getCode(), BookingStatus.CHECKED_IN.getCode())
                .lt(Booking::getCheckInDate, booking.getCheckOutDate())
                .gt(Booking::getCheckOutDate, booking.getCheckInDate());
        if (this.count(conflict) > 0) {
            recordMetric(BusinessMetricType.INVENTORY_CONFLICT, room.getId(), authenticatedUserId, null,
                    "同房同日期库存冲突已拦截");
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "所选日期内客房已被预订");
        }

        PricingQuote quote = pricingService.quote(room, booking.getCheckInDate(), booking.getCheckOutDate());

        booking.setOrderNo("ORD" + System.currentTimeMillis()
                + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase());
        booking.setUserId(authenticatedUserId);
        booking.setIdempotencyKey(idempotencyKey);
        booking.setRoomName(room.getRoomName());
        booking.setRoomNumber(room.getRoomNumber());
        booking.setDays(Math.toIntExact(quote.getNights()));
        booking.setBasePrice(quote.getBaseNightlyPrice());
        booking.setPrice(quote.getAverageNightlyPrice());
        booking.setTotalAmount(quote.getTotalAmount());
        booking.setPricingStrategyVersion(quote.getStrategyVersion());
        booking.setPricingSnapshot(pricingService.serialize(quote));
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

    private Booking replayExisting(Booking existing, Booking requested, Long userId) {
        try {
            Booking replay = requireSameRequest(existing, requested);
            recordMetric(BusinessMetricType.IDEMPOTENT_REPLAY, existing.getRoomId(), userId, existing.getId(),
                    "网络重试返回原订单");
            return replay;
        } catch (BusinessException exception) {
            recordMetric(BusinessMetricType.IDEMPOTENCY_KEY_CONFLICT,
                    existing.getRoomId(), userId, existing.getId(), "同一幂等键用于不同预订请求");
            throw exception;
        }
    }

    private void recordMetric(String eventType, Long roomId, Long userId, Long bookingId, String detail) {
        try {
            businessMetricService.record(eventType, roomId, userId, bookingId, detail);
        } catch (RuntimeException exception) {
            log.warn("业务指标记录失败，不影响主预订流程: type={}", eventType, exception);
        }
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
        if (room.getStatus() == RoomStatus.MAINTENANCE.getCode()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "维护中的客房不能确认预订");
        }
        booking.setStatus(BookingStatus.CONFIRMED.getCode());
        this.updateById(booking);
        if (room.getStatus() == RoomStatus.AVAILABLE.getCode()) {
            room.setStatus(RoomStatus.RESERVED.getCode());
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
        if (room.getStatus() == RoomStatus.MAINTENANCE.getCode()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "维护中的客房不能办理入住");
        }
        if (room.getStatus() == RoomStatus.CLEANING.getCode()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "客房清洁完成前不能办理入住");
        }
        if (room.getStatus() == RoomStatus.OCCUPIED.getCode()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "客房当前已有住客");
        }

        booking.setGuestName(guestName);
        booking.setGuestIdCard(guestIdCard);
        booking.setDeposit(deposit);
        booking.setActualCheckInTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.CHECKED_IN.getCode());
        this.updateById(booking);

        room.setStatus(RoomStatus.OCCUPIED.getCode());
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
        LocalDateTime checkedOutTime = LocalDateTime.now();
        booking.setActualCheckOutTime(checkedOutTime);
        booking.setStatus(BookingStatus.CHECKED_OUT.getCode());
        this.updateById(booking);
        eventPublisher.publishEvent(new BookingCheckedOutEvent(
                booking.getId(), booking.getRoomId(), booking.getRoomNumber(), checkedOutTime));
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
        if (room.getStatus() == RoomStatus.MAINTENANCE.getCode()
                || room.getStatus() == RoomStatus.CLEANING.getCode()) {
            return;
        }
        LambdaQueryWrapper<Booking> checkedIn = new LambdaQueryWrapper<>();
        checkedIn.eq(Booking::getRoomId, roomId).eq(Booking::getStatus, BookingStatus.CHECKED_IN.getCode());
        LambdaQueryWrapper<Booking> confirmed = new LambdaQueryWrapper<>();
        confirmed.eq(Booking::getRoomId, roomId).eq(Booking::getStatus, BookingStatus.CONFIRMED.getCode());
        room.setStatus(this.count(checkedIn) > 0 ? RoomStatus.OCCUPIED.getCode()
                : this.count(confirmed) > 0 ? RoomStatus.RESERVED.getCode() : RoomStatus.AVAILABLE.getCode());
        roomService.updateById(room);
    }
}
