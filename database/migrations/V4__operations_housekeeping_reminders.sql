-- 新增待清洁房态，清洁完成前不进入可售库存。
ALTER TABLE room
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
        COMMENT '1可预订 2已预订 3已入住 4维护中 5待清洁';

-- 退房清洁任务：同一订单只生成一个退房清洁任务，保证事件重放幂等。
CREATE TABLE housekeeping_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_no VARCHAR(40) NOT NULL,
    booking_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    room_number VARCHAR(20) NULL,
    task_type VARCHAR(40) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1清洁中 2已完成 3已取消',
    priority TINYINT NOT NULL DEFAULT 1 COMMENT '1普通 2紧急',
    assignee VARCHAR(50) NULL,
    due_time DATETIME NOT NULL,
    started_time DATETIME NULL,
    completed_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_housekeeping_task_no (task_no),
    UNIQUE KEY uk_housekeeping_booking_type (booking_id, task_type),
    KEY idx_housekeeping_status_due (status, due_time),
    KEY idx_housekeeping_room_status (room_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 运营提醒：reminder_key 防止每次定时扫描重复插入同一风险。
CREATE TABLE operation_reminder (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reminder_key VARCHAR(100) NOT NULL,
    reminder_type VARCHAR(40) NOT NULL,
    level TINYINT NOT NULL DEFAULT 1 COMMENT '1提醒 2紧急',
    title VARCHAR(100) NOT NULL,
    content VARCHAR(500) NOT NULL,
    booking_id BIGINT NULL,
    room_id BIGINT NULL,
    task_id BIGINT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已解决',
    trigger_time DATETIME NOT NULL,
    resolved_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_operation_reminder_key (reminder_key),
    KEY idx_operation_reminder_status_level (status, level, trigger_time),
    KEY idx_operation_reminder_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
