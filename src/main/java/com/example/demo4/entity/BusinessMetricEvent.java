package com.example.demo4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("business_metric_event")
public class BusinessMetricEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventType;
    private Long roomId;
    private Long userId;
    private Long bookingId;
    private String detail;
    private LocalDateTime createTime;
}
