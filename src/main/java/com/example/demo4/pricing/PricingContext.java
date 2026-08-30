package com.example.demo4.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PricingContext {
    private LocalDate stayDate;
    private BigDecimal basePrice;
    private BigDecimal occupancyRate;
    private long advanceDays;
    private long stayDays;
}
