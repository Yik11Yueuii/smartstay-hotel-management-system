package com.example.demo4.pricing;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PricingQuoteRequest {
    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}
