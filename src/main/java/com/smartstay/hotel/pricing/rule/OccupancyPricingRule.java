package com.smartstay.hotel.pricing.rule;

import com.smartstay.hotel.pricing.PricingAdjustment;
import com.smartstay.hotel.pricing.PricingContext;
import com.smartstay.hotel.pricing.PricingRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Order(20)
public class OccupancyPricingRule implements PricingRule {
    @Override
    public Optional<PricingAdjustment> evaluate(PricingContext context) {
        BigDecimal occupancy = context.getOccupancyRate();
        if (occupancy.compareTo(new BigDecimal("0.80")) >= 0) {
            return Optional.of(new PricingAdjustment("HIGH_OCCUPANCY", "高入住率",
                    "预测入住率达到80%，价格上浮20%", new BigDecimal("0.20")));
        }
        if (occupancy.compareTo(new BigDecimal("0.60")) >= 0) {
            return Optional.of(new PricingAdjustment("MEDIUM_OCCUPANCY", "入住率增长",
                    "预测入住率达到60%，价格上浮10%", new BigDecimal("0.10")));
        }
        if (occupancy.compareTo(new BigDecimal("0.30")) < 0) {
            return Optional.of(new PricingAdjustment("LOW_OCCUPANCY", "低入住率促销",
                    "预测入住率低于30%，价格下调8%", new BigDecimal("-0.08")));
        }
        return Optional.empty();
    }
}
