package com.smartstay.hotel.pricing;

import com.smartstay.hotel.pricing.rule.AdvanceBookingPricingRule;
import com.smartstay.hotel.pricing.rule.LongStayPricingRule;
import com.smartstay.hotel.pricing.rule.OccupancyPricingRule;
import com.smartstay.hotel.pricing.rule.WeekendPricingRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingRuleTest {

    @Test
    void weekendRuleRaisesFridayPrice() {
        PricingAdjustment adjustment = new WeekendPricingRule().evaluate(
                context(LocalDate.of(2026, 9, 4), "0.50", 10, 1)).orElseThrow();

        assertEquals("WEEKEND", adjustment.getRuleCode());
        assertEquals(new BigDecimal("0.15"), adjustment.getRate());
    }

    @Test
    void occupancyRuleUsesThreeExplainableBands() {
        OccupancyPricingRule rule = new OccupancyPricingRule();

        assertEquals("HIGH_OCCUPANCY", rule.evaluate(context(LocalDate.now(), "0.80", 10, 1))
                .orElseThrow().getRuleCode());
        assertEquals("MEDIUM_OCCUPANCY", rule.evaluate(context(LocalDate.now(), "0.60", 10, 1))
                .orElseThrow().getRuleCode());
        assertEquals("LOW_OCCUPANCY", rule.evaluate(context(LocalDate.now(), "0.29", 10, 1))
                .orElseThrow().getRuleCode());
        assertTrue(rule.evaluate(context(LocalDate.now(), "0.40", 10, 1)).isEmpty());
    }

    @Test
    void advanceAndLongStayRulesApplyDiscounts() {
        PricingContext context = context(LocalDate.now(), "0.50", 21, 5);

        assertEquals(new BigDecimal("-0.08"), new AdvanceBookingPricingRule().evaluate(context)
                .orElseThrow().getRate());
        assertEquals(new BigDecimal("-0.08"), new LongStayPricingRule().evaluate(context)
                .orElseThrow().getRate());
    }

    private PricingContext context(LocalDate date, String occupancy, long advanceDays, long stayDays) {
        return new PricingContext(date, new BigDecimal("400.00"), new BigDecimal(occupancy), advanceDays, stayDays);
    }
}
