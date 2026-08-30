package com.smartstay.hotel.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class PricingNightQuote {
    private LocalDate stayDate;
    private BigDecimal basePrice;
    private BigDecimal occupancyRate;
    private List<PricingAdjustment> adjustments;
    private BigDecimal finalPrice;
}
