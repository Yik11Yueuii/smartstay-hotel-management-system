package com.example.demo4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo4.common.BookingStatus;
import com.example.demo4.common.HousekeepingStatus;
import com.example.demo4.entity.Booking;
import com.example.demo4.entity.HousekeepingTask;
import com.example.demo4.entity.OperationReminder;
import com.example.demo4.exception.BusinessException;
import com.example.demo4.exception.ErrorCode;
import com.example.demo4.mapper.BookingMapper;
import com.example.demo4.mapper.OperationReminderMapper;
import com.example.demo4.service.HousekeepingService;
import com.example.demo4.service.OperationReminderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationReminderServiceImpl extends ServiceImpl<OperationReminderMapper, OperationReminder>
        implements OperationReminderService {
    public static final String CLEANING_OVERDUE = "CLEANING_OVERDUE";
    public static final String CHECKIN_CLEANING_RISK = "CHECKIN_CLEANING_RISK";

    private final HousekeepingService housekeepingService;
    private final BookingMapper bookingMapper;

    public OperationReminderServiceImpl(HousekeepingService housekeepingService, BookingMapper bookingMapper) {
        this.housekeepingService = housekeepingService;
        this.bookingMapper = bookingMapper;
    }

    @Override
    @Transactional
    public int scanAndCreate(LocalDateTime now) {
        List<HousekeepingTask> activeTasks = housekeepingService.list(
                new LambdaQueryWrapper<HousekeepingTask>()
                        .in(HousekeepingTask::getStatus,
                                HousekeepingStatus.PENDING.getCode(), HousekeepingStatus.IN_PROGRESS.getCode()));
        int created = 0;
        for (HousekeepingTask task : activeTasks) {
            if (task.getDueTime() != null && !task.getDueTime().isAfter(now)) {
                long overdueMinutes = Math.max(0, Duration.between(task.getDueTime(), now).toMinutes());
                int level = overdueMinutes >= 30 ? 2 : 1;
                created += createOrEscalate(
                        CLEANING_OVERDUE + ":" + task.getId(), CLEANING_OVERDUE, level,
                        "退房清洁已逾期",
                        "房间 " + task.getRoomNumber() + " 清洁任务已逾期 " + overdueMinutes + " 分钟",
                        task.getBookingId(), task.getRoomId(), task.getId(), task.getDueTime()) ? 1 : 0;
            }

            List<Booking> arrivals = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                    .eq(Booking::getRoomId, task.getRoomId())
                    .eq(Booking::getStatus, BookingStatus.CONFIRMED.getCode())
                    .ge(Booking::getCheckInDate, now.toLocalDate())
                    .le(Booking::getCheckInDate, now.toLocalDate().plusDays(1)));
            for (Booking booking : arrivals) {
                created += createOrEscalate(
                        CHECKIN_CLEANING_RISK + ":" + booking.getId(), CHECKIN_CLEANING_RISK, 2,
                        "临近入住但客房尚未清洁",
                        "订单 " + booking.getOrderNo() + " 将于 " + booking.getCheckInDate()
                                + " 入住，房间 " + task.getRoomNumber() + " 仍有未完成清洁任务",
                        booking.getId(), task.getRoomId(), task.getId(), now) ? 1 : 0;
            }
        }
        return created;
    }

    @Override
    @Transactional
    public void resolve(Long reminderId) {
        OperationReminder reminder = getById(reminderId);
        if (reminder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "运营提醒不存在");
        }
        if (Integer.valueOf(1).equals(reminder.getStatus())) return;
        reminder.setStatus(1);
        reminder.setResolvedTime(LocalDateTime.now());
        updateById(reminder);
    }

    @Override
    @Transactional
    public void resolveByTask(Long taskId, LocalDateTime resolvedTime) {
        update(new LambdaUpdateWrapper<OperationReminder>()
                .eq(OperationReminder::getTaskId, taskId)
                .eq(OperationReminder::getStatus, 0)
                .set(OperationReminder::getStatus, 1)
                .set(OperationReminder::getResolvedTime, resolvedTime));
    }

    private boolean createOrEscalate(String key, String type, int level, String title, String content,
                                     Long bookingId, Long roomId, Long taskId, LocalDateTime triggerTime) {
        OperationReminder existing = getOne(new LambdaQueryWrapper<OperationReminder>()
                .eq(OperationReminder::getReminderKey, key), false);
        if (existing != null) {
            if (Integer.valueOf(0).equals(existing.getStatus())
                    && (existing.getLevel() == null || existing.getLevel() < level)) {
                existing.setLevel(level);
                existing.setContent(content);
                updateById(existing);
            }
            return false;
        }

        OperationReminder reminder = new OperationReminder();
        reminder.setReminderKey(key);
        reminder.setReminderType(type);
        reminder.setLevel(level);
        reminder.setTitle(title);
        reminder.setContent(content);
        reminder.setBookingId(bookingId);
        reminder.setRoomId(roomId);
        reminder.setTaskId(taskId);
        reminder.setStatus(0);
        reminder.setTriggerTime(triggerTime);
        try {
            return save(reminder);
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }
}
