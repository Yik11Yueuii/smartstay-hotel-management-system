package com.smartstay.hotel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartstay.hotel.entity.Room;
import com.smartstay.hotel.mapper.RoomMapper;
import com.smartstay.hotel.service.RoomService;
import org.springframework.stereotype.Service;

@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {
}