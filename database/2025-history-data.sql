-- 2025 年历史经营数据（非破坏、幂等）
-- 用途：让 2025 年 12 月完成的系统拥有可解释的全年经营样本。
-- 可重复执行；仅插入 HIST-2025-* 前缀的数据，不修改现有订单与房态。

SET NAMES utf8mb4;

INSERT INTO booking (
    order_no, idempotency_key, user_id, room_id, room_name, room_number,
    check_in_date, check_out_date, days, base_price, price, total_amount,
    pricing_strategy_version, pricing_snapshot,
    contact_name, contact_phone, guest_name, guest_id_card,
    deposit, actual_check_in_time, actual_check_out_time,
    deposit_return, additional_charges, remark, status, create_time, update_time
)
SELECT
    CONCAT('HIST-2025-', LPAD(sample_no, 4, '0')),
    CONCAT('history-2025-', LPAD(sample_no, 4, '0')),
    u.id,
    r.id,
    r.room_name,
    r.room_number,
    check_in_date,
    DATE_ADD(check_in_date, INTERVAL stay_days DAY),
    stay_days,
    r.price,
    ROUND(r.price * price_factor, 2),
    ROUND(r.price * price_factor * stay_days, 2),
    CASE WHEN MONTH(check_in_date) = 12 THEN 'SMART_PRICING_V1' ELSE NULL END,
    CASE WHEN MONTH(check_in_date) = 12 THEN JSON_OBJECT(
        'source', '2025-history-demo',
        'basePrice', r.price,
        'factor', price_factor,
        'reason', '十二月经营演示价'
    ) ELSE NULL END,
    u.nickname,
    u.phone,
    CASE WHEN MOD(sample_no, 9) = 0 THEN NULL ELSE u.nickname END,
    CASE WHEN MOD(sample_no, 9) = 0 THEN NULL ELSE CONCAT('3101011990', LPAD(sample_no, 8, '0')) END,
    CASE WHEN MOD(sample_no, 9) = 0 THEN 0.00 ELSE 300.00 END,
    CASE WHEN MOD(sample_no, 9) = 0 THEN NULL ELSE TIMESTAMP(check_in_date, '14:00:00') END,
    CASE WHEN MOD(sample_no, 9) = 0 THEN NULL ELSE TIMESTAMP(DATE_ADD(check_in_date, INTERVAL stay_days DAY), '11:00:00') END,
    CASE WHEN MOD(sample_no, 9) = 0 THEN 0.00 ELSE 300.00 END,
    CASE WHEN MOD(sample_no, 13) = 0 AND MOD(sample_no, 9) <> 0 THEN 38.00 ELSE 0.00 END,
    CASE
        WHEN MOD(sample_no, 9) = 0 THEN '2025 历史演示：客户取消'
        WHEN MOD(sample_no, 13) = 0 THEN '2025 历史演示：含迷你吧消费'
        ELSE '2025 全年经营样本'
    END,
    CASE WHEN MOD(sample_no, 9) = 0 THEN 2 ELSE 4 END,
    TIMESTAMP(DATE_SUB(check_in_date, INTERVAL (5 + MOD(sample_no, 15)) DAY), '10:00:00'),
    CASE
        WHEN MOD(sample_no, 9) = 0 THEN TIMESTAMP(DATE_SUB(check_in_date, INTERVAL 3 DAY), '16:00:00')
        ELSE TIMESTAMP(DATE_ADD(check_in_date, INTERVAL stay_days DAY), '11:00:00')
    END
FROM (
    SELECT dated.*,
           CAST(
               (CASE
                    WHEN MONTH(check_in_date) IN (1, 2) THEN 0.92
                    WHEN MONTH(check_in_date) IN (7, 8) THEN 1.12
                    WHEN MONTH(check_in_date) = 10 THEN 1.18
                    WHEN MONTH(check_in_date) = 12 THEN 1.08
                    ELSE 1.00
                END)
                + (CASE WHEN DAYOFWEEK(check_in_date) IN (6, 7) THEN 0.08 ELSE 0.00 END)
               AS DECIMAL(4,2)
           ) AS price_factor
    FROM (
        SELECT seq.sample_no,
               DATE_ADD('2025-01-15', INTERVAL ((seq.sample_no - 1) * 5) DAY) AS check_in_date,
               1 + MOD(seq.sample_no, 3) AS stay_days,
               1 + MOD(seq.sample_no - 1, 11) AS room_id,
               CASE MOD(seq.sample_no - 1, 3)
                   WHEN 0 THEN 'user1'
                   WHEN 1 THEN 'user2'
                   ELSE 'user3'
               END AS username
        FROM (
            SELECT ones.n + tens.n * 10 + 1 AS sample_no
            FROM (
                SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
            ) ones
            CROSS JOIN (
                SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
                UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
            ) tens
        ) seq
        WHERE seq.sample_no <= 70
    ) dated
) samples
JOIN user u ON u.username = samples.username
JOIN room r ON r.id = samples.room_id
WHERE NOT EXISTS (
    SELECT 1 FROM booking existing
    WHERE existing.order_no = CONCAT('HIST-2025-', LPAD(samples.sample_no, 4, '0'))
);

-- 为部分已退房订单生成已完成的清洁记录，形成订单 -> 退房 -> 清洁历史链路。
INSERT INTO housekeeping_task (
    task_no, booking_id, room_id, room_number, task_type, status, priority,
    assignee, due_time, started_time, completed_time, create_time, update_time
)
SELECT
    CONCAT('HIST-HK-2025-', RIGHT(b.order_no, 4)),
    b.id,
    b.room_id,
    b.room_number,
    'CHECKOUT_CLEANING',
    2,
    1,
    CASE MOD(CAST(RIGHT(b.order_no, 4) AS UNSIGNED), 3)
        WHEN 0 THEN '保洁员-陈姐'
        WHEN 1 THEN '保洁员-李姐'
        ELSE '保洁员-王姐'
    END,
    DATE_ADD(b.actual_check_out_time, INTERVAL 30 MINUTE),
    DATE_ADD(b.actual_check_out_time, INTERVAL 10 MINUTE),
    DATE_ADD(b.actual_check_out_time, INTERVAL 35 MINUTE),
    b.actual_check_out_time,
    DATE_ADD(b.actual_check_out_time, INTERVAL 35 MINUTE)
FROM booking b
WHERE b.order_no LIKE 'HIST-2025-%'
  AND b.status = 4
  AND MOD(CAST(RIGHT(b.order_no, 4) AS UNSIGNED), 4) = 0
  AND NOT EXISTS (
      SELECT 1 FROM housekeeping_task existing
      WHERE existing.task_no = CONCAT('HIST-HK-2025-', RIGHT(b.order_no, 4))
  );

-- 15 条全年客户反馈，标题前缀用于幂等判断和精确回滚。
INSERT INTO feedback (user_id, title, content, type, status, reply, create_time, update_time)
SELECT
    u.id,
    CONCAT('[2025历史] #', LPAD(seed.sample_no, 2, '0'), ' ', seed.title),
    seed.content,
    seed.feedback_type,
    1,
    seed.reply,
    seed.created_at,
    DATE_ADD(seed.created_at, INTERVAL 1 DAY)
FROM (
    SELECT 1 sample_no, '入住办理很顺畅' title, '前台核验和入住办理速度很快。' content, 0 feedback_type, '感谢认可，我们会继续保持。' reply, 'user1' username, '2025-01-20 18:20:00' created_at
    UNION ALL SELECT 2, '房间安静整洁', '房间卫生不错，夜间也很安静。', 0, '感谢您的反馈。', 'user2', '2025-02-18 09:30:00'
    UNION ALL SELECT 3, '建议增加早餐提示', '入住后希望能收到早餐时间和地点提醒。', 2, '已纳入运营提醒优化清单。', 'user3', '2025-03-16 10:10:00'
    UNION ALL SELECT 4, '退房效率很高', '押金退还清晰，退房过程很快。', 0, '感谢认可。', 'user1', '2025-04-22 12:05:00'
    UNION ALL SELECT 5, '空调响应较慢', '房间降温速度偏慢，希望检查设备。', 1, '工程人员已完成空调滤网检查。', 'user2', '2025-05-19 21:15:00'
    UNION ALL SELECT 6, '家庭房体验不错', '空间适合亲子入住，床品也很舒适。', 0, '期待您再次入住。', 'user3', '2025-06-14 15:40:00'
    UNION ALL SELECT 7, '暑期价格说明清楚', '预订时能看到价格调整原因，容易理解。', 0, '感谢对透明定价的认可。', 'user1', '2025-07-21 11:25:00'
    UNION ALL SELECT 8, '建议增加停车位提示', '希望预订页面提前显示剩余停车位。', 2, '已记录，后续将评估停车信息联动。', 'user2', '2025-08-17 08:50:00'
    UNION ALL SELECT 9, '清洁速度很快', '续住期间提出清洁需求后处理及时。', 0, '感谢您的认可。', 'user3', '2025-09-12 17:35:00'
    UNION ALL SELECT 10, '国庆入住有序', '客流量大但前台办理仍然有序。', 0, '感谢您的耐心与认可。', 'user1', '2025-10-04 19:10:00'
    UNION ALL SELECT 11, '热水温度稳定', '高峰时段热水供应稳定。', 0, '感谢反馈。', 'user2', '2025-10-26 22:05:00'
    UNION ALL SELECT 12, '建议优化发票入口', '希望退房后能更快找到开票入口。', 2, '已记录到产品改进清单。', 'user3', '2025-11-11 13:45:00'
    UNION ALL SELECT 13, '十二月定价合理', '周末价格变化有规则说明，体验透明。', 0, '感谢对智慧定价功能的认可。', 'user1', '2025-12-07 16:30:00'
    UNION ALL SELECT 14, '退房后房间整理及时', '遗落物品很快就收到前台通知。', 0, '感谢认可，清洁任务已形成闭环。', 'user2', '2025-12-18 14:20:00'
    UNION ALL SELECT 15, '整体体验满意', '预订、入住和退房流程完整顺畅。', 0, '期待再次为您服务。', 'user3', '2025-12-27 12:10:00'
) seed
JOIN user u ON u.username = seed.username
WHERE NOT EXISTS (
    SELECT 1 FROM feedback existing
    WHERE existing.title = CONCAT('[2025历史] #', LPAD(seed.sample_no, 2, '0'), ' ', seed.title)
);

SELECT YEAR(check_in_date) AS year,
       COUNT(*) AS booking_count,
       SUM(status = 4) AS checked_out_count,
       SUM(status = 2) AS cancelled_count,
       ROUND(SUM(CASE WHEN status = 4 THEN total_amount ELSE 0 END), 2) AS completed_revenue
FROM booking
WHERE order_no LIKE 'HIST-2025-%'
GROUP BY YEAR(check_in_date);

