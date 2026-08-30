package com.smartstay.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartstay.hotel.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}