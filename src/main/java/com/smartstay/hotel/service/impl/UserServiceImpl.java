package com.smartstay.hotel.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartstay.hotel.entity.User;
import com.smartstay.hotel.mapper.UserMapper;
import com.smartstay.hotel.service.UserService;
import com.smartstay.hotel.service.AuthTokenService;
import com.smartstay.hotel.exception.BusinessException;
import com.smartstay.hotel.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public UserServiceImpl(PasswordEncoder passwordEncoder, AuthTokenService authTokenService) {
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "用户名或密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.getOne(wrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }

        String storedPassword = user.getPassword();
        boolean bcryptPassword = storedPassword != null && storedPassword.startsWith("$2");
        boolean passwordMatches = bcryptPassword
                ? passwordEncoder.matches(password, storedPassword)
                : password.equals(storedPassword);

        if (!passwordMatches) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        // Upgrade legacy plaintext passwords after a successful login.
        if (!bcryptPassword) {
            user.setPassword(passwordEncoder.encode(password));
            this.updateById(user);
        }

        Map<String, Object> result = new HashMap<>();
        user.setPassword(null);
        result.put("user", user);
        result.put("token", authTokenService.createToken(user));

        return result;
    }

    @Override
    public void register(User user) {
        if (StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "用户名或密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        this.save(user);
    }

    @Override
    public User updateProfile(Long userId, String nickname, String phone) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (StrUtil.isNotBlank(phone) && !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "手机号格式不正确");
        }
        user.setNickname(StrUtil.trim(nickname));
        user.setPhone(StrUtil.trim(phone));
        this.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "原密码和新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "新密码长度应为6-20个字符");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }
}
