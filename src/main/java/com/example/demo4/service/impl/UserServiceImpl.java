package com.example.demo4.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo4.entity.User;
import com.example.demo4.mapper.UserMapper;
import com.example.demo4.service.UserService;
import com.example.demo4.service.AuthTokenService;
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
            throw new RuntimeException("用户名或密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.getOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String storedPassword = user.getPassword();
        boolean bcryptPassword = storedPassword != null && storedPassword.startsWith("$2");
        boolean passwordMatches = bcryptPassword
                ? passwordEncoder.matches(password, storedPassword)
                : password.equals(storedPassword);

        if (!passwordMatches) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
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
            throw new RuntimeException("用户名或密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        this.save(user);
    }
}
