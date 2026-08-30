package com.smartstay.hotel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartstay.hotel.entity.Notice;
import com.smartstay.hotel.mapper.NoticeMapper;
import com.smartstay.hotel.service.NoticeService;
import org.springframework.stereotype.Service;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {
}