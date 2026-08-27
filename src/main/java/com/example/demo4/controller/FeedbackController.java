package com.example.demo4.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo4.common.Result;
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
    public Result<Page<Feedback>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId) {

        Page<Feedback> pageInfo = new Page<>(page, size);
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Feedback::getStatus, status);
        }

        if (userId != null) {
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
    public Result<Feedback> getById(@PathVariable Long id) {
        Feedback feedback = feedbackService.getById(id);
        return Result.success(feedback);
    }

    /**
     * 提交反馈
     */
    @PostMapping("/create")
    public Result<String> create(@RequestBody Feedback feedback) {
        feedback.setStatus(0);
        feedbackService.save(feedback);
        return Result.success("提交成功");
    }

    /**
     * 回复反馈
     */
    @PutMapping("/reply")
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
    public Result<String> delete(@PathVariable Long id) {
        feedbackService.removeById(id);
        return Result.success("删除成功");
    }
}