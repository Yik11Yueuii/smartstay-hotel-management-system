package com.example.demo4.config;

import com.example.demo4.common.AuthPrincipal;
import com.example.demo4.common.Result;
import com.example.demo4.exception.ErrorCode;
import com.example.demo4.service.AuthTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTH_USER_ID = "authUserId";
    public static final String AUTH_USER_ROLE = "authUserRole";
    private final AuthTokenService tokenService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(AuthTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                AuthPrincipal principal = tokenService.verify(authorization.substring(7));
                String role = principal.getRole() == 1 ? "ROLE_ADMIN" : "ROLE_USER";
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, Collections.singletonList(new SimpleGrantedAuthority(role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(AUTH_USER_ID, principal.getUserId());
                request.setAttribute(AUTH_USER_ROLE, principal.getRole());
            } catch (IllegalArgumentException exception) {
                writeError(response, ErrorCode.UNAUTHORIZED, exception.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        response.setStatus(code.getHttpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(code.getHttpStatus(), code.getCode(), message)));
    }
}
