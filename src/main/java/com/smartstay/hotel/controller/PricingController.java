package com.smartstay.hotel.controller;

import com.smartstay.hotel.common.Result;
import com.smartstay.hotel.exception.BusinessException;
import com.smartstay.hotel.exception.ErrorCode;
import com.smartstay.hotel.pricing.PricingQuote;
import com.smartstay.hotel.pricing.PricingQuoteRequest;
import com.smartstay.hotel.pricing.PricingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {
    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/quote")
    public Result<PricingQuote> quote(@RequestBody PricingQuoteRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "报价参数不能为空");
        }
        return Result.success(pricingService.quote(
                request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate()));
    }
}
