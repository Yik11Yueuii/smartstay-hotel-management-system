package com.smartstay.hotel.common;

public final class BusinessMetricType {
    public static final String INVENTORY_CONFLICT = "INVENTORY_CONFLICT";
    public static final String IDEMPOTENT_REPLAY = "IDEMPOTENT_REPLAY";
    public static final String IDEMPOTENCY_KEY_CONFLICT = "IDEMPOTENCY_KEY_CONFLICT";

    private BusinessMetricType() {
    }
}
