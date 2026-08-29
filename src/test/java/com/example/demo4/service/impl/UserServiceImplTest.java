package com.example.demo4.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.demo4.entity.User;
import com.example.demo4.service.AuthTokenService;
import com.example.demo4.common.AuthPrincipal;
import com.example.demo4.exception.BusinessException;
import com.example.demo4.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class UserServiceImplTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void loginAcceptsBcryptPasswordAndDoesNotReturnPassword() {
        User user = enabledUser(passwordEncoder.encode("admin123"));
        UserServiceImpl service = serviceReturning(user);

        Map<String, Object> result = service.login("admin", "admin123");

        User returnedUser = (User) result.get("user");
        assertEquals(1L, returnedUser.getId());
        assertNull(returnedUser.getPassword());
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = enabledUser(passwordEncoder.encode("admin123"));
        UserServiceImpl service = serviceReturning(user);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.login("admin", "wrong-password")
        );

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    void loginReturnsTokenThatRestoresUserAndRole() {
        AuthTokenService tokenService = new AuthTokenService("test-secret-with-at-least-32-characters", 1);
        User user = enabledUser(passwordEncoder.encode("admin123"));
        UserServiceImpl service = spy(new UserServiceImpl(passwordEncoder, tokenService));
        doReturn(user).when(service).getOne(any(Wrapper.class));

        String token = (String) service.login("admin", "admin123").get("token");
        AuthPrincipal principal = tokenService.verify(token);

        assertEquals(1L, principal.getUserId());
        assertEquals(1, principal.getRole());
    }

    @Test
    void loginUpgradesLegacyPlaintextPassword() {
        User user = enabledUser("admin123");
        UserServiceImpl service = serviceReturning(user);
        doReturn(true).when(service).updateById(any(User.class));

        service.login("admin", "admin123");

        verify(service).updateById(any(User.class));
    }

    @Test
    void updateProfileChangesOnlyAllowedProfileFields() {
        User user = enabledUser(passwordEncoder.encode("admin123"));
        UserServiceImpl service = spy(new UserServiceImpl(passwordEncoder, mock(AuthTokenService.class)));
        doReturn(user).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(User.class));

        User updated = service.updateProfile(1L, "新昵称", "13800000000");

        assertEquals("新昵称", updated.getNickname());
        assertEquals("13800000000", updated.getPhone());
        assertEquals(1, updated.getRole());
        assertNull(updated.getPassword());
    }

    @Test
    void changePasswordVerifiesOldPasswordAndHashesNewPassword() {
        User user = enabledUser(passwordEncoder.encode("admin123"));
        UserServiceImpl service = spy(new UserServiceImpl(passwordEncoder, mock(AuthTokenService.class)));
        doReturn(user).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(User.class));

        service.changePassword(1L, "admin123", "newPassword123");

        assertTrue(passwordEncoder.matches("newPassword123", user.getPassword()));
    }

    @Test
    void updateProfileRejectsInvalidPhone() {
        User user = enabledUser(passwordEncoder.encode("admin123"));
        UserServiceImpl service = spy(new UserServiceImpl(passwordEncoder, mock(AuthTokenService.class)));
        doReturn(user).when(service).getById(1L);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.updateProfile(1L, "昵称", "123"));

        assertEquals("手机号格式不正确", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private UserServiceImpl serviceReturning(User user) {
        UserServiceImpl service = spy(new UserServiceImpl(passwordEncoder, mock(AuthTokenService.class)));
        doReturn(user).when(service).getOne(any(Wrapper.class));
        return service;
    }

    private User enabledUser(String password) {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword(password);
        user.setRole(1);
        user.setStatus(1);
        return user;
    }
}
