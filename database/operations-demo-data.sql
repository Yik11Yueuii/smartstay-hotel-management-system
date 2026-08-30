-- 在已执行 V4 迁移的演示库中补充一个逾期清洁任务，便于直接展示运营提醒。
USE hotel_management;
SET NAMES utf8mb4;

INSERT INTO housekeeping_task
    (task_no, booking_id, room_id, room_number, task_type, status, priority, due_time, create_time)
SELECT 'DEMO-HK-ROOM-0401', b.id, b.room_id, b.room_number, 'CHECKOUT_CLEANING', 0, 2,
       COALESCE(b.actual_check_out_time, NOW()) + INTERVAL 30 MINUTE,
       COALESCE(b.actual_check_out_time, NOW())
FROM booking b
WHERE b.order_no = 'DEMO-CHECKEDOUT-001'
  AND NOT EXISTS (
      SELECT 1 FROM housekeeping_task h
      WHERE h.booking_id = b.id AND h.task_type = 'CHECKOUT_CLEANING'
  );

UPDATE room r
JOIN booking b ON b.room_id = r.id AND b.order_no = 'DEMO-CHECKEDOUT-001'
JOIN housekeeping_task h ON h.booking_id = b.id AND h.status IN (0, 1)
SET r.status = 5
WHERE r.status <> 4;
