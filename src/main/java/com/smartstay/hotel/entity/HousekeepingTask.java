package com.smartstay.hotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("housekeeping_task")
public class HousekeepingTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private Long bookingId;
    private Long roomId;
    private String roomNumber;
    private String taskType;
    private Integer status;
    private Integer priority;
    private String assignee;
    private LocalDateTime dueTime;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
