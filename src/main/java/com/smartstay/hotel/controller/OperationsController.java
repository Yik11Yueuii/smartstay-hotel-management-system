package com.smartstay.hotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartstay.hotel.common.HousekeepingStatus;
import com.smartstay.hotel.common.Result;
import com.smartstay.hotel.entity.HousekeepingTask;
import com.smartstay.hotel.entity.OperationReminder;
import com.smartstay.hotel.service.HousekeepingService;
import com.smartstay.hotel.service.OperationReminderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operations")
public class OperationsController {
    private final HousekeepingService housekeepingService;
    private final OperationReminderService reminderService;

    public OperationsController(HousekeepingService housekeepingService,
                                OperationReminderService reminderService) {
        this.housekeepingService = housekeepingService;
        this.reminderService = reminderService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        LocalDateTime now = LocalDateTime.now();
        List<HousekeepingTask> tasks = housekeepingService.list(
                new LambdaQueryWrapper<HousekeepingTask>()
                        .orderByAsc(HousekeepingTask::getStatus)
                        .orderByDesc(HousekeepingTask::getCreateTime)
                        .last("LIMIT 50"));
        List<OperationReminder> reminders = reminderService.list(
                new LambdaQueryWrapper<OperationReminder>()
                        .eq(OperationReminder::getStatus, 0)
                        .orderByDesc(OperationReminder::getLevel)
                        .orderByDesc(OperationReminder::getTriggerTime)
                        .last("LIMIT 50"));

        Map<String, Object> result = new HashMap<>();
        result.put("tasks", tasks);
        result.put("reminders", reminders);
        result.put("pendingTasks", housekeepingService.count(new LambdaQueryWrapper<HousekeepingTask>()
                .eq(HousekeepingTask::getStatus, HousekeepingStatus.PENDING.getCode())));
        result.put("inProgressTasks", housekeepingService.count(new LambdaQueryWrapper<HousekeepingTask>()
                .eq(HousekeepingTask::getStatus, HousekeepingStatus.IN_PROGRESS.getCode())));
        result.put("overdueTasks", housekeepingService.count(new LambdaQueryWrapper<HousekeepingTask>()
                .in(HousekeepingTask::getStatus,
                        HousekeepingStatus.PENDING.getCode(), HousekeepingStatus.IN_PROGRESS.getCode())
                .le(HousekeepingTask::getDueTime, now)));
        result.put("openReminders", reminderService.count(new LambdaQueryWrapper<OperationReminder>()
                .eq(OperationReminder::getStatus, 0)));
        return Result.success(result);
    }

    @PutMapping("/tasks/{taskId}/start")
    public Result<HousekeepingTask> start(@PathVariable Long taskId,
                                          @RequestBody(required = false) Map<String, String> params) {
        String assignee = params == null ? null : params.get("assignee");
        return Result.success(housekeepingService.startTask(taskId, assignee));
    }

    @PutMapping("/tasks/{taskId}/complete")
    public Result<HousekeepingTask> complete(@PathVariable Long taskId) {
        return Result.success(housekeepingService.completeTask(taskId));
    }

    @PutMapping("/reminders/{reminderId}/resolve")
    public Result<String> resolve(@PathVariable Long reminderId) {
        reminderService.resolve(reminderId);
        return Result.success("提醒已处理");
    }

    @PostMapping("/reminders/scan")
    public Result<Map<String, Integer>> scan() {
        return Result.success(Map.of("created", reminderService.scanAndCreate(LocalDateTime.now())));
    }
}
