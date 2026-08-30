package com.example.demo4.pricing;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.demo4.entity.Room;
import com.example.demo4.exception.BusinessException;
import com.example.demo4.mapper.BookingMapper;
import com.example.demo4.pricing.rule.AdvanceBookingPricingRule;
import com.example.demo4.pricing.rule.LongStayPricingRule;
import com.example.demo4.pricing.rule.OccupancyPricingRule;
import com.example.demo4.pricing.rule.WeekendPricingRule;
import com.example.demo4.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PricingServiceTest {

    @Test
    void quoteCombinesRulesAndReturnsDailyExplanation() {
        RoomService roomService = mock(RoomService.class);
        BookingMapper bookingMapper = mock(BookingMapper.class);
        Room room = room();
        when(roomService.count(any(Wrapper.class))).thenReturn(10L);
        when(bookingMapper.countOccupiedRooms(any(LocalDate.class))).thenReturn(8L);
        PricingService service = new PricingService(List.of(
                new WeekendPricingRule(),
                new OccupancyPricingRule(),
                new AdvanceBookingPricingRule(),
                new LongStayPricingRule()), roomService, bookingMapper, new ObjectMapper());
        LocalDate friday = LocalDate.now().plusDays(21)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

        PricingQuote quote = service.quote(room, friday, friday.plusDays(1));

        assertEquals("SMART_PRICING_V1", quote.getStrategyVersion());
        assertEquals(new BigDecimal("508.00"), quote.getTotalAmount());
        assertEquals(new BigDecimal("0.8000"), quote.getNightlyPrices().get(0).getOccupancyRate());
        assertEquals(List.of("WEEKEND", "HIGH_OCCUPANCY", "EARLY_BIRD_21"),
                quote.getNightlyPrices().get(0).getAdjustments().stream()
                        .map(PricingAdjustment::getRuleCode).toList());
    }

    @Test
    void quoteRejectsInvalidDateRange() {
        PricingService service = new PricingService(List.of(), mock(RoomService.class),
                mock(BookingMapper.class), new ObjectMapper());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.quote(room(), LocalDate.now().plusDays(2), LocalDate.now().plusDays(2)));

        assertTrue(exception.getMessage().contains("退房日期"));
    }

    private Room room() {
        Room room = new Room();
        room.setId(8L);
        room.setStatus(1);
        room.setPrice(new BigDecimal("400.00"));
        room.setIsPromotion(0);
        return room;
    }
}
