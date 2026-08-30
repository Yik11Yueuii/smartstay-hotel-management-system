package com.smartstay.hotel.common;

import com.smartstay.hotel.exception.BusinessException;
import com.smartstay.hotel.exception.ErrorCode;

public enum HousekeepingStatus {
    PENDING(0, "待处理"),
    IN_PROGRESS(1, "清洁中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String label;

    HousekeepingStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static HousekeepingStatus fromCode(Integer code) {
        for (HousekeepingStatus status : values()) {
            if (Integer.valueOf(status.code).equals(code)) return status;
        }
        throw new BusinessException(ErrorCode.STATE_CONFLICT, "未知清洁任务状态");
    }

    public void requireTransitionTo(HousekeepingStatus target) {
        boolean allowed = (this == PENDING && (target == IN_PROGRESS || target == COMPLETED || target == CANCELLED))
                || (this == IN_PROGRESS && (target == COMPLETED || target == CANCELLED));
        if (!allowed) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT,
                    "清洁任务不能从“" + label + "”变更为“" + target.label + "”");
        }
    }
}
