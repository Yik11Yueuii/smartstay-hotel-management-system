package com.example.demo4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("room")
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomName;
    private String roomType;
    private String roomNumber;
    private BigDecimal price;
    private String bedType;
    private Integer maxPeople;
    private String description;
    private Integer status;
    private Integer isPromotion;
    private BigDecimal promotionPrice;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
