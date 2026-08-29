package com.example.demo4.common;

import com.example.demo4.exception.BusinessException;
import com.example.demo4.exception.ErrorCode;

import java.util.EnumSet;
import java.util.Set;

public enum BookingStatus {
    PENDING(0, "待确认"),
    CONFIRMED(1, "已确认"),
    CANCELLED(2, "已取消"),
    CHECKED_IN(3, "已入住"),
    CHECKED_OUT(4, "已退房");

    private final int code;
    private final String description;

    BookingStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static BookingStatus fromCode(Integer code) {
        for (BookingStatus status : values()) {
            if (Integer.valueOf(status.code).equals(code)) return status;
        }
        throw new BusinessException(ErrorCode.STATE_CONFLICT, "未知订单状态: " + code);
    }

    public void requireTransitionTo(BookingStatus target) {
        if (!allowedTargets().contains(target)) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "订单不能从“" + description + "”变更为“" + target.description + "”");
        }
    }

    private Set<BookingStatus> allowedTargets() {
        switch (this) {
            case PENDING: return EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED: return EnumSet.of(CANCELLED, CHECKED_IN);
            case CHECKED_IN: return EnumSet.of(CHECKED_OUT);
            default: return EnumSet.noneOf(BookingStatus.class);
        }
    }
}
