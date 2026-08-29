package com.example.demo4.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo4.common.Result;
import com.example.demo4.config.JwtAuthenticationFilter;
import com.example.demo4.entity.User;
import com.example.demo4.service.UserService;
import com.example.demo4.exception.BusinessException;
import com.example.demo4.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        return Result.success(userService.login(params.get("username"), params.get("password")));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        userService.register(user);
        return Result.success("注册成功");
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(@RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/me")
    public Result<User> updateCurrentUser(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long userId,
            @RequestBody Map<String, String> params) {
        return Result.success(userService.updateProfile(userId, params.get("nickname"), params.get("phone")));
    }

    @PutMapping("/me/password")
    public Result<String> updateCurrentUserPassword(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long userId,
            @RequestBody Map<String, String> params) {
        userService.changePassword(userId, params.get("oldPassword"), params.get("newPassword"));
        return Result.success("密码修改成功");
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public Result<String> update(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long authenticatedUserId,
            @RequestBody User user) {
        if (user.getId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "用户ID不能为空");
        }
        if (authenticatedUserId.equals(user.getId())
                && (Integer.valueOf(0).equals(user.getStatus())
                || (user.getRole() != null && !Integer.valueOf(1).equals(user.getRole())))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能禁用当前管理员或取消其管理员角色");
        }
        user.setPassword(null);
        userService.updateById(user);
        return Result.success("更新成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<String> updatePassword(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long authenticatedUserId,
            @RequestBody Map<String, Object> params) {
        Object rawUserId = params.get("userId");
        if (rawUserId == null) throw new BusinessException(ErrorCode.INVALID_REQUEST, "用户ID不能为空");
        Long userId = Long.valueOf(rawUserId.toString());
            if (!authenticatedUserId.equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "不能修改其他用户的密码");
            }
            String oldPassword = params.get("oldPassword").toString();
            String newPassword = params.get("newPassword").toString();

            userService.changePassword(userId, oldPassword, newPassword);
        return Result.success("密码修改成功");
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username) {

        Page<User> pageInfo = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (username != null && !username.isEmpty()) {
            wrapper.like(User::getUsername, username)
                    .or()
                    .like(User::getNickname, username);
        }

        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userService.page(pageInfo, wrapper);

        // 不返回密码
        result.getRecords().forEach(user -> user.setPassword(null));

        return Result.success(result);
    }
}
