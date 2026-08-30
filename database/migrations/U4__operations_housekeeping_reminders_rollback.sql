-- 回滚前应先确认不再需要历史清洁任务与运营提醒。
DROP TABLE IF EXISTS operation_reminder;
DROP TABLE IF EXISTS housekeeping_task;

-- 旧版本不识别房态 5，回滚时安全降级为维护中，避免未经清洁直接恢复销售。
UPDATE room SET status = 4 WHERE status = 5;
ALTER TABLE room
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
        COMMENT '1可预订 2已预订 3已入住 4维护中';
