package com.example.demo4.exception;

public enum ErrorCode {
    INVALID_REQUEST(400, "INVALID_REQUEST", "请求参数不合法"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "请先登录"),
    FORBIDDEN(403, "FORBIDDEN", "无权执行此操作"),
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "资源不存在"),
    STATE_CONFLICT(409, "STATE_CONFLICT", "当前状态不允许此操作"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "服务器内部错误");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
