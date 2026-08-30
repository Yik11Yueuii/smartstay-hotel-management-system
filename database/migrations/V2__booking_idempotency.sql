-- 幂等预订：同一用户的同一个请求键最多创建一张订单。
ALTER TABLE booking
    ADD COLUMN idempotency_key VARCHAR(64) NULL COMMENT '客户端预订幂等键' AFTER order_no,
    ADD UNIQUE KEY uk_booking_user_idempotency (user_id, idempotency_key),
    ADD KEY idx_booking_room_dates (room_id, check_in_date, check_out_date, status);
