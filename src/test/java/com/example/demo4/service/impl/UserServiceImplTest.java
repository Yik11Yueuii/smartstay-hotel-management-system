package com.example.demo4.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.demo4.entity.User;
import com.example.demo4.service.AuthTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.login("admin", "wrong-password")
        );

        assertEquals("密码错误", exception.getMessage());
    }

    @Test
    void loginUpgradesLegacyPlaintextPassword() {
        User user = enabledUser("admin123");
        UserServiceImpl service = serviceReturning(user);
        doReturn(true).when(service).updateById(any(User.class));

        service.login("admin", "admin123");

        verify(service).updateById(any(User.class));
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
