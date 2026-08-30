package com.example.demo4.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PricingAdjustment {
    private String ruleCode;
    private String ruleName;
    private String reason;
    private BigDecimal rate;
    private BigDecimal amount;

    public PricingAdjustment(String ruleCode, String ruleName, String reason, BigDecimal rate) {
        this(ruleCode, ruleName, reason, rate, BigDecimal.ZERO);
    }
}
