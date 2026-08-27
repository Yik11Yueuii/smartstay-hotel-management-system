package com.example.demo4.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo4.common.Result;
import com.example.demo4.entity.Notice;
import com.example.demo4.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notice")
@CrossOrigin
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 分页查询公告列表
     */
    @GetMapping("/list")
    public Result<Page<Notice>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {

        Page<Notice> pageInfo = new Page<>(page, size);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Notice::getStatus, status);
        }

        wrapper.orderByDesc(Notice::getCreateTime);
        Page<Notice> result = noticeService.page(pageInfo, wrapper);

        return Result.success(result);
    }

    /**
     * 获取显示的公告(前台)
     */
    @GetMapping("/show")
    public Result<Page<Notice>> getShowNotices(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Page<Notice> pageInfo = new Page<>(page, size);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getStatus, 1);
        wrapper.orderByDesc(Notice::getCreateTime);

        Page<Notice> result = noticeService.page(pageInfo, wrapper);
        return Result.success(result);
    }

    /**
     * 获取公告详情
     */
    @GetMapping("/{id}")
    public Result<Notice> getById(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        return Result.success(notice);
    }

    /**
     * 添加公告
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Notice notice) {
        noticeService.save(notice);
        return Result.success("添加成功");
    }

    /**
     * 更新公告
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody Notice notice) {
        noticeService.updateById(notice);
        return Result.success("更新成功");
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success("删除成功");
    }
}