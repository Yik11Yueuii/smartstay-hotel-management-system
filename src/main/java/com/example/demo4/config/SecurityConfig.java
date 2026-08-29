package com.example.demo4.config;

import com.example.demo4.common.Result;
import com.example.demo4.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                            ObjectMapper objectMapper) throws Exception {
        http.csrf().disable().cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                .antMatchers("/", "/pages/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .antMatchers("/api/user/login", "/api/user/register").permitAll()
                .antMatchers(HttpMethod.GET, "/api/room/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/notice/show").permitAll()
                .regexMatchers(HttpMethod.GET, "^/api/notice/[0-9]+$").permitAll()
                .antMatchers("/api/user/me", "/api/user/me/**", "/api/user/password").authenticated()
                .antMatchers("/api/dashboard/**", "/api/notice/list", "/api/notice/add", "/api/notice/update",
                        "/api/booking/confirm/**", "/api/booking/checkin", "/api/booking/checkout",
                        "/api/user/list", "/api/user/update", "/api/user/*", "/api/feedback/reply").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/room/**", "/api/notice/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/room/**", "/api/notice/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/room/**", "/api/notice/**", "/api/feedback/**").hasRole("ADMIN")
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll().and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, exception) ->
                        writeError(response, objectMapper, ErrorCode.UNAUTHORIZED))
                .accessDeniedHandler((request, response, exception) ->
                        writeError(response, objectMapper, ErrorCode.FORBIDDEN));
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeError(javax.servlet.http.HttpServletResponse response, ObjectMapper mapper,
                                   ErrorCode code) throws java.io.IOException {
        response.setStatus(code.getHttpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(mapper.writeValueAsString(
                Result.error(code.getHttpStatus(), code.getCode(), code.getDefaultMessage())));
    }
}
