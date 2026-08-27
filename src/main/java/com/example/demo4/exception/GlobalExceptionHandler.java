package com.example.demo4.exception;

import com.example.demo4.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getCode())
                .body(Result.error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Result<Object>> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest().body(Result.error(400, "请求参数格式错误"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleUnexpectedException(Exception exception) {
        return ResponseEntity.status(500).body(Result.error(500, "服务器内部错误"));
    }
}
