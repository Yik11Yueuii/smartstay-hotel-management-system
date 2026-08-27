package com.example.demo4.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo4.common.Result;
import com.example.demo4.config.AdminOnly;
import com.example.demo4.config.AuthInterceptor;
import com.example.demo4.config.LoginRequired;
import com.example.demo4.entity.Feedback;
import com.example.demo4.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    /**
     * 分页查询反馈列表
     */
    @GetMapping("/list")
    @LoginRequired
    public Result<Page<Feedback>> list(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ID) Long authenticatedUserId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ROLE) Integer authenticatedRole,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId) {

        Page<Feedback> pageInfo = new Page<>(page, size);
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Feedback::getStatus, status);
        }

        if (authenticatedRole != 1) {
            wrapper.eq(Feedback::getUserId, authenticatedUserId);
        } else if (userId != null) {
            wrapper.eq(Feedback::getUserId, userId);
        }

        wrapper.orderByDesc(Feedback::getCreateTime);
        Page<Feedback> result = feedbackService.page(pageInfo, wrapper);

        return Result.success(result);
    }

    /**
     * 获取反馈详情
     */
    @GetMapping("/{id}")
    @LoginRequired
    public Result<Feedback> getById(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ID) Long authenticatedUserId,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ROLE) Integer authenticatedRole,
            @PathVariable Long id) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback != null && authenticatedRole != 1
                && !authenticatedUserId.equals(feedback.getUserId())) {
            return Result.error("不能查看其他用户的反馈");
        }
        return Result.success(feedback);
    }

    /**
     * 提交反馈
     */
    @PostMapping("/create")
    @LoginRequired
    public Result<String> create(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ID) Long authenticatedUserId,
            @RequestBody Feedback feedback) {
        feedback.setUserId(authenticatedUserId);
        feedback.setStatus(0);
        feedbackService.save(feedback);
        return Result.success("提交成功");
    }

    /**
     * 回复反馈
     */
    @PutMapping("/reply")
    @AdminOnly
    public Result<String> reply(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            String reply = params.get("reply").toString();

            Feedback feedback = feedbackService.getById(id);
            if (feedback == null) {
                return Result.error("反馈不存在");
            }

            feedback.setReply(reply);
            feedback.setStatus(1);
            feedbackService.updateById(feedback);

            return Result.success("回复成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除反馈
     */
    @DeleteMapping("/{id}")
    @AdminOnly
    public Result<String> delete(@PathVariable Long id) {
        feedbackService.removeById(id);
        return Result.success("删除成功");
    }
}
