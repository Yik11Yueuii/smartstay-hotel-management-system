package com.smartstay.hotel.pricing;

import java.util.Optional;

public interface PricingRule {
    Optional<PricingAdjustment> evaluate(PricingContext context);
}
