package com.smartstay.hotel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartstay.hotel.entity.Feedback;
import com.smartstay.hotel.mapper.FeedbackMapper;
import com.smartstay.hotel.service.FeedbackService;
import org.springframework.stereotype.Service;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {
}