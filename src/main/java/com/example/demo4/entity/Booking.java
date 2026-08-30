package com.example.demo4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("booking")
public class Booking {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    @JsonIgnore
    private String idempotencyKey;
    private Long userId;
    private Long roomId;
    private String roomName;
    private String roomNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;

    private Integer days;
    private BigDecimal basePrice;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String pricingStrategyVersion;
    @JsonIgnore
    private String pricingSnapshot;
    private String contactName;
    private String contactPhone;
    private String guestName;
    private String guestIdCard;
    private BigDecimal deposit;
    private LocalDateTime actualCheckInTime;
    private LocalDateTime actualCheckOutTime;
    private BigDecimal depositReturn;
    private BigDecimal additionalCharges;
    private String remark;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
