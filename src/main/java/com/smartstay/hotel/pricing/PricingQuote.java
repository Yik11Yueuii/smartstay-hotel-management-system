package com.smartstay.hotel.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class PricingQuote {
    private String strategyVersion;
    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private long nights;
    private BigDecimal baseNightlyPrice;
    private BigDecimal averageNightlyPrice;
    private BigDecimal totalAmount;
    private List<PricingNightQuote> nightlyPrices;
}
