package com.example.demo4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo4.entity.Room;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoomMapper extends BaseMapper<Room> {
}