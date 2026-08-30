package com.smartstay.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartstay.hotel.entity.User;
import java.util.Map;

public interface UserService extends IService<User> {
    Map<String, Object> login(String username, String password);
    void register(User user);
    User updateProfile(Long userId, String nickname, String phone);
    void changePassword(Long userId, String oldPassword, String newPassword);
}
