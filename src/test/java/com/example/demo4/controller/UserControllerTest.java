package com.example.demo4.controller;

import com.example.demo4.common.Result;
import com.example.demo4.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserControllerTest {

    @Test
    void currentAdminCannotDisableOwnAccount() {
        UserController controller = new UserController();
        User update = new User();
        update.setId(1L);
        update.setStatus(0);

        Result<String> result = controller.update(1L, update);

        assertEquals(400, result.getCode());
        assertEquals("不能禁用当前管理员或取消其管理员角色", result.getMessage());
    }

    @Test
    void currentAdminCannotRemoveOwnAdminRole() {
        UserController controller = new UserController();
        User update = new User();
        update.setId(1L);
        update.setRole(0);

        Result<String> result = controller.update(1L, update);

        assertEquals(400, result.getCode());
        assertEquals("不能禁用当前管理员或取消其管理员角色", result.getMessage());
    }
}
