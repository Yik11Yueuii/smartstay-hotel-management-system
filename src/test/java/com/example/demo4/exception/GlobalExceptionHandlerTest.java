package com.example.demo4.exception;

import com.example.demo4.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsStableBusinessErrorCodeAndHttpStatus() {
        ResponseEntity<Result<Object>> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.STATE_CONFLICT, "只有已确认订单可以办理入住"));

        assertEquals(409, response.getStatusCodeValue());
        assertEquals(409, response.getBody().getCode());
        assertEquals("STATE_CONFLICT", response.getBody().getErrorCode());
        assertEquals("只有已确认订单可以办理入住", response.getBody().getMessage());
    }

    @Test
    void hidesUnexpectedExceptionDetailsFromClient() {
        ResponseEntity<Result<Object>> response = handler.handleUnexpectedException(
                new IllegalStateException("database password leaked in stack trace"));

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        assertEquals("服务器内部错误", response.getBody().getMessage());
    }
}
