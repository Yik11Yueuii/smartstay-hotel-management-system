package com.smartstay.hotel.controller;

import com.smartstay.hotel.entity.User;
import com.smartstay.hotel.exception.BusinessException;
import com.smartstay.hotel.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserControllerTest {

    @Test
    void currentAdminCannotDisableOwnAccount() {
        UserController controller = new UserController();
        User update = new User();
        update.setId(1L);
        update.setStatus(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.update(1L, update));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        assertEquals("不能禁用当前管理员或取消其管理员角色", exception.getMessage());
    }

    @Test
    void currentAdminCannotRemoveOwnAdminRole() {
        UserController controller = new UserController();
        User update = new User();
        update.setId(1L);
        update.setRole(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.update(1L, update));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        assertEquals("不能禁用当前管理员或取消其管理员角色", exception.getMessage());
    }
}
