package com.example.demo4.config;

import com.example.demo4.common.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.http.server.ServletServerHttpResponse;

@ControllerAdvice
public class ResultStatusAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof Result && response instanceof ServletServerHttpResponse) {
            Integer code = ((Result<?>) body).getCode();
            if (code != null && code >= 400 && code <= 599) {
                ((ServletServerHttpResponse) response).getServletResponse().setStatus(code);
            }
        }
        return body;
    }
}
