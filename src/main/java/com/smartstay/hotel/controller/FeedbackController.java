package com.smartstay.hotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartstay.hotel.common.Result;
import com.smartstay.hotel.config.JwtAuthenticationFilter;
import com.smartstay.hotel.entity.Feedback;
import com.smartstay.hotel.service.FeedbackService;
import com.smartstay.hotel.exception.BusinessException;
import com.smartstay.hotel.exception.ErrorCode;
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
    public Result<Page<Feedback>> list(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long authenticatedUserId,
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ROLE) Integer authenticatedRole,
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
    public Result<Feedback> getById(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long authenticatedUserId,
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ROLE) Integer authenticatedRole,
            @PathVariable Long id) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "反馈不存在");
        }
        if (authenticatedRole != 1
                && !authenticatedUserId.equals(feedback.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能查看其他用户的反馈");
        }
        return Result.success(feedback);
    }

    /**
     * 提交反馈
     */
    @PostMapping("/create")
    public Result<String> create(
            @RequestAttribute(JwtAuthenticationFilter.AUTH_USER_ID) Long authenticatedUserId,
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
    public Result<String> reply(@RequestBody Map<String, Object> params) {
        Object rawId = params.get("id");
        if (rawId == null) throw new BusinessException(ErrorCode.INVALID_REQUEST, "反馈ID不能为空");
        Long id = Long.valueOf(rawId.toString());
            String reply = params.get("reply").toString();

            Feedback feedback = feedbackService.getById(id);
            if (feedback == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "反馈不存在");
            }

            feedback.setReply(reply);
            feedback.setStatus(1);
            feedbackService.updateById(feedback);

        return Result.success("回复成功");
    }

    /**
     * 删除反馈
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        feedbackService.removeById(id);
        return Result.success("删除成功");
    }
}
