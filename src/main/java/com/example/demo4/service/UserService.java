package com.example.demo4.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo4.entity.User;
import java.util.Map;

public interface UserService extends IService<User> {
    Map<String, Object> login(String username, String password);
    void register(User user);
    User updateProfile(Long userId, String nickname, String phone);
    void changePassword(Long userId, String oldPassword, String newPassword);
}
