package com.smartstay.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartstay.hotel.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoomMapper extends BaseMapper<Room> {
    @Select("SELECT * FROM room WHERE id = #{id} FOR UPDATE")
    Room selectByIdForUpdate(Long id);
}
