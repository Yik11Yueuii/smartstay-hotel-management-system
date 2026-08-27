package com.example.demo4.config;

import com.example.demo4.common.Result;
import com.example.demo4.common.AuthPrincipal;
import com.example.demo4.service.AuthTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String AUTH_USER_ID = "authUserId";
    public static final String AUTH_USER_ROLE = "authUserRole";

    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AuthTokenService authTokenService, ObjectMapper objectMapper) {
        this.authTokenService = authTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        boolean adminOnly = handler instanceof HandlerMethod
                && ((HandlerMethod) handler).hasMethodAnnotation(AdminOnly.class);
        boolean loginRequired = handler instanceof HandlerMethod
                && ((HandlerMethod) handler).hasMethodAnnotation(LoginRequired.class);
        boolean userApi = request.getRequestURI().startsWith("/api/user/");
        boolean bookingApi = request.getRequestURI().startsWith("/api/booking/");
        if (!userApi && !bookingApi && !adminOnly && !loginRequired) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, 401, "请先登录");
            return false;
        }

        try {
            AuthPrincipal principal = authTokenService.verify(authorization.substring(7));
            request.setAttribute(AUTH_USER_ID, principal.getUserId());
            request.setAttribute(AUTH_USER_ROLE, principal.getRole());

            if (adminOnly && principal.getRole() != 1) {
                writeError(response, 403, "需要管理员权限");
                return false;
            }
            return true;
        } catch (IllegalArgumentException exception) {
            writeError(response, 401, exception.getMessage());
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
