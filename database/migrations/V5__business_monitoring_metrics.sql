-- 记录真正影响经营与可靠性的业务事件；接口耗时使用进程内滚动窗口，不写入业务库。
CREATE TABLE business_metric_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    room_id BIGINT NULL,
    user_id BIGINT NULL,
    booking_id BIGINT NULL,
    detail VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_business_metric_type_time (event_type, create_time),
    KEY idx_business_metric_room_time (room_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
