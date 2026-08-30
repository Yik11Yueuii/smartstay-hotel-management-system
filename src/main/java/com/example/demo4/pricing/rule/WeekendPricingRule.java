package com.example.demo4.pricing.rule;

import com.example.demo4.pricing.PricingAdjustment;
import com.example.demo4.pricing.PricingContext;
import com.example.demo4.pricing.PricingRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Optional;

@Component
@Order(10)
public class WeekendPricingRule implements PricingRule {
    @Override
    public Optional<PricingAdjustment> evaluate(PricingContext context) {
        DayOfWeek day = context.getStayDate().getDayOfWeek();
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
            return Optional.of(new PricingAdjustment("WEEKEND", "周末需求",
                    "周五或周六入住，需求上浮15%", new BigDecimal("0.15")));
        }
        return Optional.empty();
    }
}
