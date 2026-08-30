package com.smartstay.hotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartstay.hotel.common.Result;
import com.smartstay.hotel.entity.Room;
import com.smartstay.hotel.service.RoomService;
import com.smartstay.hotel.exception.BusinessException;
import com.smartstay.hotel.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room")
@CrossOrigin
public class RoomController {

    @Autowired
    private RoomService roomService;

    /**
     * 分页查询客房列表
     */
    @GetMapping("/list")
    public Result<Page<Room>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer status) {

        Page<Room> pageInfo = new Page<>(page, size);
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();

        if (roomName != null && !roomName.isEmpty()) {
            wrapper.like(Room::getRoomName, roomName);
        }

        if (roomType != null && !roomType.isEmpty()) {
            wrapper.eq(Room::getRoomType, roomType);
        }

        if (status != null) {
            wrapper.eq(Room::getStatus, status);
        }

        wrapper.orderByDesc(Room::getCreateTime);
        Page<Room> result = roomService.page(pageInfo, wrapper);

        return Result.success(result);
    }

    /**
     * 获取促销客房
     */
    @GetMapping("/promotion")
    public Result<Page<Room>> getPromotionRooms(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Page<Room> pageInfo = new Page<>(page, size);
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Room::getIsPromotion, 1);
        wrapper.eq(Room::getStatus, 1);
        wrapper.orderByDesc(Room::getCreateTime);

        Page<Room> result = roomService.page(pageInfo, wrapper);
        return Result.success(result);
    }

    /**
     * 获取客房详情
     */
    @GetMapping("/{id}")
    public Result<Room> getById(@PathVariable Long id) {
        Room room = roomService.getById(id);
        if (room == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "客房不存在");
        }
        return Result.success(room);
    }

    /**
     * 添加客房
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Room room) {
        // 设置默认值
            if (room.getStatus() == null) {
                room.setStatus(1); // 默认可预订
            }
            if (room.getIsPromotion() == null) {
                room.setIsPromotion(0); // 默认不促销
            }

        if (!roomService.save(room)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "客房添加失败");
        }
        return Result.success("添加成功");
    }


    /**
     * 更新客房
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody Room room) {
        roomService.updateById(room);
        return Result.success("更新成功");
    }

    /**
     * 删除客房
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        roomService.removeById(id);
        return Result.success("删除成功");
    }
}
