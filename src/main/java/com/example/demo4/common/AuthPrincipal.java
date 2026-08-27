package com.example.demo4.common;

public class AuthPrincipal {
    private final Long userId;
    private final Integer role;

    public AuthPrincipal(Long userId, Integer role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getRole() {
        return role;
    }
}
