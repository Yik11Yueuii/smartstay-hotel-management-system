package com.example.demo4.pricing.rule;

import com.example.demo4.pricing.PricingAdjustment;
import com.example.demo4.pricing.PricingContext;
import com.example.demo4.pricing.PricingRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Order(30)
public class AdvanceBookingPricingRule implements PricingRule {
    @Override
    public Optional<PricingAdjustment> evaluate(PricingContext context) {
        if (context.getAdvanceDays() >= 21) {
            return Optional.of(new PricingAdjustment("EARLY_BIRD_21", "提前预订",
                    "提前21天以上预订，价格下调8%", new BigDecimal("-0.08")));
        }
        if (context.getAdvanceDays() >= 14) {
            return Optional.of(new PricingAdjustment("EARLY_BIRD_14", "提前预订",
                    "提前14天以上预订，价格下调5%", new BigDecimal("-0.05")));
        }
        if (context.getAdvanceDays() <= 2) {
            return Optional.of(new PricingAdjustment("LAST_MINUTE", "临近入住",
                    "距入住不足3天，价格上浮10%", new BigDecimal("0.10")));
        }
        return Optional.empty();
    }
}
