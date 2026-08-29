package com.example.demo4.exception;

import com.example.demo4.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getCode())
                .body(Result.error(exception.getCode(), exception.getErrorCode().getCode(), exception.getMessage()));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            NumberFormatException.class
    })
    public ResponseEntity<Result<Object>> handleBadRequest(Exception exception) {
        ErrorCode code = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.badRequest().body(Result.error(code.getHttpStatus(), code.getCode(), "请求参数格式错误"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled server exception", exception);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(500).body(Result.error(code.getHttpStatus(), code.getCode(), code.getDefaultMessage()));
    }
}
