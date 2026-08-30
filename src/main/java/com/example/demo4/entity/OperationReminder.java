package com.example.demo4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_reminder")
public class OperationReminder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reminderKey;
    private String reminderType;
    private Integer level;
    private String title;
    private String content;
    private Long bookingId;
    private Long roomId;
    private Long taskId;
    private Integer status;
    private LocalDateTime triggerTime;
    private LocalDateTime resolvedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
