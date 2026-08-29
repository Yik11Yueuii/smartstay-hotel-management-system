package com.example.demo4.exception;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(String message) {
        this(ErrorCode.INVALID_REQUEST, message);
    }

    public BusinessException(int code, String message) {
        this(resolve(code), message);
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getCode() {
        return errorCode.getHttpStatus();
    }

    public ErrorCode getErrorCode() { return errorCode; }

    private static ErrorCode resolve(int status) {
        for (ErrorCode value : ErrorCode.values()) {
            if (value.getHttpStatus() == status) return value;
        }
        return ErrorCode.INVALID_REQUEST;
    }
}
