package com.example.demo4.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo4.common.HousekeepingStatus;
import com.example.demo4.common.RoomStatus;
import com.example.demo4.entity.HousekeepingTask;
import com.example.demo4.entity.Room;
import com.example.demo4.exception.BusinessException;
import com.example.demo4.exception.ErrorCode;
import com.example.demo4.mapper.HousekeepingTaskMapper;
import com.example.demo4.operations.event.BookingCheckedOutEvent;
import com.example.demo4.operations.event.HousekeepingCompletedEvent;
import com.example.demo4.service.HousekeepingService;
import com.example.demo4.service.RoomService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class HousekeepingServiceImpl extends ServiceImpl<HousekeepingTaskMapper, HousekeepingTask>
        implements HousekeepingService {
    public static final String CHECKOUT_CLEANING = "CHECKOUT_CLEANING";

    private final RoomService roomService;
    private final ApplicationEventPublisher eventPublisher;

    public HousekeepingServiceImpl(RoomService roomService, ApplicationEventPublisher eventPublisher) {
        this.roomService = roomService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public HousekeepingTask createCheckoutTask(BookingCheckedOutEvent event) {
        HousekeepingTask existing = findByBooking(event.bookingId());
        if (existing != null) return existing;

        HousekeepingTask task = new HousekeepingTask();
        task.setTaskNo("HK" + System.currentTimeMillis()
                + IdUtil.fastSimpleUUID().substring(0, 5).toUpperCase());
        task.setBookingId(event.bookingId());
        task.setRoomId(event.roomId());
        task.setRoomNumber(event.roomNumber());
        task.setTaskType(CHECKOUT_CLEANING);
        task.setStatus(HousekeepingStatus.PENDING.getCode());
        task.setPriority(1);
        LocalDateTime checkoutTime = event.checkedOutTime() == null ? LocalDateTime.now() : event.checkedOutTime();
        task.setDueTime(checkoutTime.plusMinutes(30));
        try {
            if (!save(task)) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "清洁任务创建失败");
            }
        } catch (DuplicateKeyException exception) {
            return findByBooking(event.bookingId());
        }

        Room room = roomService.getById(event.roomId());
        if (room == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "退房客房不存在");
        }
        if (!Integer.valueOf(RoomStatus.MAINTENANCE.getCode()).equals(room.getStatus())) {
            room.setStatus(RoomStatus.CLEANING.getCode());
            roomService.updateById(room);
        }
        return task;
    }

    @Override
    @Transactional
    public HousekeepingTask startTask(Long taskId, String assignee) {
        HousekeepingTask task = requireTask(taskId);
        HousekeepingStatus.fromCode(task.getStatus()).requireTransitionTo(HousekeepingStatus.IN_PROGRESS);
        String actualAssignee = StrUtil.isBlank(assignee) ? "保洁组" : assignee.trim();
        boolean updated = update(new LambdaUpdateWrapper<HousekeepingTask>()
                .eq(HousekeepingTask::getId, taskId)
                .eq(HousekeepingTask::getStatus, task.getStatus())
                .set(HousekeepingTask::getStatus, HousekeepingStatus.IN_PROGRESS.getCode())
                .set(HousekeepingTask::getAssignee, actualAssignee)
                .set(HousekeepingTask::getStartedTime, LocalDateTime.now()));
        if (!updated) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "清洁任务状态已变化，请刷新后重试");
        }
        return requireTask(taskId);
    }

    @Override
    @Transactional
    public HousekeepingTask completeTask(Long taskId) {
        HousekeepingTask task = requireTask(taskId);
        HousekeepingStatus.fromCode(task.getStatus()).requireTransitionTo(HousekeepingStatus.COMPLETED);
        LocalDateTime completedTime = LocalDateTime.now();
        boolean updated = update(new LambdaUpdateWrapper<HousekeepingTask>()
                .eq(HousekeepingTask::getId, taskId)
                .eq(HousekeepingTask::getStatus, task.getStatus())
                .set(HousekeepingTask::getStatus, HousekeepingStatus.COMPLETED.getCode())
                .set(task.getStartedTime() == null, HousekeepingTask::getStartedTime, completedTime)
                .set(StrUtil.isBlank(task.getAssignee()), HousekeepingTask::getAssignee, "保洁组")
                .set(HousekeepingTask::getCompletedTime, completedTime));
        if (!updated) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "清洁任务状态已变化，请刷新后重试");
        }
        eventPublisher.publishEvent(new HousekeepingCompletedEvent(task.getId(), task.getRoomId()));
        return requireTask(taskId);
    }

    @Override
    public boolean hasActiveTask(Long roomId) {
        return count(new LambdaQueryWrapper<HousekeepingTask>()
                .eq(HousekeepingTask::getRoomId, roomId)
                .in(HousekeepingTask::getStatus,
                        HousekeepingStatus.PENDING.getCode(), HousekeepingStatus.IN_PROGRESS.getCode())) > 0;
    }

    private HousekeepingTask findByBooking(Long bookingId) {
        return getOne(new LambdaQueryWrapper<HousekeepingTask>()
                .eq(HousekeepingTask::getBookingId, bookingId)
                .eq(HousekeepingTask::getTaskType, CHECKOUT_CLEANING), false);
    }

    private HousekeepingTask requireTask(Long taskId) {
        HousekeepingTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "清洁任务不存在");
        }
        return task;
    }
}
