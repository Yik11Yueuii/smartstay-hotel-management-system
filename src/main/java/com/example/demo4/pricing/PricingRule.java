package com.example.demo4.pricing;

import java.util.Optional;

public interface PricingRule {
    Optional<PricingAdjustment> evaluate(PricingContext context);
}
