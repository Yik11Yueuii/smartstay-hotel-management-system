package com.example.demo4.pricing.rule;

import com.example.demo4.pricing.PricingAdjustment;
import com.example.demo4.pricing.PricingContext;
import com.example.demo4.pricing.PricingRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Order(40)
public class LongStayPricingRule implements PricingRule {
    @Override
    public Optional<PricingAdjustment> evaluate(PricingContext context) {
        if (context.getStayDays() >= 5) {
            return Optional.of(new PricingAdjustment("LONG_STAY_5", "连住优惠",
                    "连续入住5晚以上，价格下调8%", new BigDecimal("-0.08")));
        }
        if (context.getStayDays() >= 3) {
            return Optional.of(new PricingAdjustment("LONG_STAY_3", "连住优惠",
                    "连续入住3晚以上，价格下调5%", new BigDecimal("-0.05")));
        }
        return Optional.empty();
    }
}
