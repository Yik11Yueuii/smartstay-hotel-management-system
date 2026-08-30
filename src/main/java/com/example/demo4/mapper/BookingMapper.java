package com.example.demo4.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo4.entity.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface BookingMapper extends BaseMapper<Booking> {
    @Select("SELECT COUNT(DISTINCT room_id) FROM booking "
            + "WHERE status IN (0, 1, 3) AND check_in_date <= #{stayDate} AND check_out_date > #{stayDate}")
    long countOccupiedRooms(@Param("stayDate") LocalDate stayDate);
}
