package com.example.demo4.pricing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo4.entity.Room;
import com.example.demo4.common.RoomStatus;
import com.example.demo4.exception.BusinessException;
import com.example.demo4.exception.ErrorCode;
import com.example.demo4.mapper.BookingMapper;
import com.example.demo4.service.RoomService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class PricingService {
    public static final String STRATEGY_VERSION = "SMART_PRICING_V1";
    private static final BigDecimal MIN_RATE = new BigDecimal("-0.30");
    private static final BigDecimal MAX_RATE = new BigDecimal("0.80");

    private final List<PricingRule> rules;
    private final RoomService roomService;
    private final BookingMapper bookingMapper;
    private final ObjectMapper objectMapper;

    public PricingService(List<PricingRule> rules, RoomService roomService,
                          BookingMapper bookingMapper, ObjectMapper objectMapper) {
        this.rules = rules;
        this.roomService = roomService;
        this.bookingMapper = bookingMapper;
        this.objectMapper = objectMapper;
    }

    public PricingQuote quote(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (roomId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "请选择客房");
        }
        Room room = roomService.getById(roomId);
        if (room == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "客房不存在");
        return quote(room, checkInDate, checkOutDate);
    }

    public PricingQuote quote(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        validateDates(checkInDate, checkOutDate);
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        long advanceDays = ChronoUnit.DAYS.between(LocalDate.now(), checkInDate);
        BigDecimal basePrice = basePrice(room);
        long sellableRooms = roomService.count(new LambdaQueryWrapper<Room>()
                .notIn(Room::getStatus, RoomStatus.MAINTENANCE.getCode(), RoomStatus.CLEANING.getCode()));
        if (sellableRooms <= 0) throw new BusinessException(ErrorCode.STATE_CONFLICT, "当前没有可售客房");

        List<PricingNightQuote> nightlyPrices = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
            BigDecimal occupancy = BigDecimal.valueOf(bookingMapper.countOccupiedRooms(date))
                    .divide(BigDecimal.valueOf(sellableRooms), 4, RoundingMode.HALF_UP)
                    .min(BigDecimal.ONE);
            PricingContext context = new PricingContext(date, basePrice, occupancy, advanceDays, nights);
            List<PricingAdjustment> adjustments = new ArrayList<>();
            BigDecimal rate = BigDecimal.ZERO;
            for (PricingRule rule : rules) {
                PricingAdjustment adjustment = rule.evaluate(context).orElse(null);
                if (adjustment != null) {
                    adjustment.setAmount(basePrice.multiply(adjustment.getRate()).setScale(2, RoundingMode.HALF_UP));
                    adjustments.add(adjustment);
                    rate = rate.add(adjustment.getRate());
                }
            }
            rate = rate.max(MIN_RATE).min(MAX_RATE);
            BigDecimal finalPrice = basePrice.multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP);
            nightlyPrices.add(new PricingNightQuote(date, basePrice, occupancy, adjustments, finalPrice));
            total = total.add(finalPrice);
        }
        BigDecimal average = total.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
        return new PricingQuote(STRATEGY_VERSION, room.getId(), checkInDate, checkOutDate, nights,
                basePrice, average, total.setScale(2, RoundingMode.HALF_UP), nightlyPrices);
    }

    public String serialize(PricingQuote quote) {
        try {
            return objectMapper.writeValueAsString(quote);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "定价快照生成失败");
        }
    }

    private BigDecimal basePrice(Room room) {
        BigDecimal price = Integer.valueOf(1).equals(room.getIsPromotion()) && room.getPromotionPrice() != null
                ? room.getPromotionPrice() : room.getPrice();
        if (price == null || price.signum() <= 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "客房基础价格无效");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "退房日期必须晚于入住日期");
        }
        if (checkInDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "入住日期不能早于今天");
        }
        if (ChronoUnit.DAYS.between(checkInDate, checkOutDate) > 30) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "单次预订最多30晚");
        }
    }
}
