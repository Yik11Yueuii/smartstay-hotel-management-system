package com.example.demo4.common;

public enum RoomStatus {
    AVAILABLE(1),
    RESERVED(2),
    OCCUPIED(3),
    MAINTENANCE(4),
    CLEANING(5);

    private final int code;

    RoomStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
