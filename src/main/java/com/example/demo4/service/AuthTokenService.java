package com.example.demo4.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import com.example.demo4.common.AuthPrincipal;
import com.example.demo4.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class AuthTokenService {
    private static final Logger log = LoggerFactory.getLogger(AuthTokenService.class);
    private static final String LOCAL_DEFAULT_SECRET = "hotel-management-local-secret-change-me";

    private final byte[] secret;
    private final int expireHours;

    public AuthTokenService(@Value("${auth.token.secret}") String secret,
                            @Value("${auth.token.expire-hours}") int expireHours) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expireHours = expireHours;
        if (LOCAL_DEFAULT_SECRET.equals(secret)) {
            log.warn("正在使用本地默认 JWT 密钥；部署环境必须设置 AUTH_TOKEN_SECRET");
        }
    }

    public String createToken(User user) {
        Date now = new Date();
        return JWT.create()
                .setSubject(String.valueOf(user.getId()))
                .setPayload("role", user.getRole())
                .setIssuedAt(now)
                .setExpiresAt(DateUtil.offsetHour(now, expireHours))
                .setKey(secret)
                .sign();
    }

    public AuthPrincipal verify(String token) {
        try {
            JWT jwt = JWT.of(token).setKey(secret);
            if (!jwt.verify() || !jwt.validate(0)) {
                throw new IllegalArgumentException("登录凭证无效或已过期");
            }
            Long userId = Long.valueOf(jwt.getPayload("sub").toString());
            Integer role = Integer.valueOf(jwt.getPayload("role").toString());
            return new AuthPrincipal(userId, role);
        } catch (Exception exception) {
            throw new IllegalArgumentException("登录凭证无效或已过期");
        }
    }

    public Long verifyAndGetUserId(String token) {
        return verify(token).getUserId();
    }
}
