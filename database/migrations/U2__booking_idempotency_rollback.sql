-- 回滚 V2。执行前确认没有依赖 idempotency_key 的外部程序。
ALTER TABLE booking
    DROP INDEX idx_booking_room_dates,
    DROP INDEX uk_booking_user_idempotency,
    DROP COLUMN idempotency_key;
