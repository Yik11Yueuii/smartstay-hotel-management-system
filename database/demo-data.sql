-- 智慧酒店管理系统演示数据
-- 警告：执行后会清空 hotel_management 中现有的 7 张业务表。
-- 演示账号：admin / admin123，普通用户 user1-user4 / 123456。

USE hotel_management;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE operation_reminder;
TRUNCATE TABLE housekeeping_task;
TRUNCATE TABLE feedback;
TRUNCATE TABLE booking;
TRUNCATE TABLE notice;
TRUNCATE TABLE room;
TRUNCATE TABLE user;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO user (id, username, password, nickname, phone, role, status, create_time) VALUES
(1, 'admin', '$2a$10$0ZmRJBeHlgh3tq5CbixhmuJXzToppNSUdsCWEmwX0RU.r8Ka8.Tzq', '系统管理员', '13800000001', 1, 1, NOW() - INTERVAL 180 DAY),
(2, 'user1', '$2a$10$ih5dmixyxaH4SQSXAJh.I.8PeJJ5wNznWLs0vEPa8RjD4sbfIUIMK', '张晨', '13800000002', 0, 1, NOW() - INTERVAL 120 DAY),
(3, 'user2', '$2a$10$ih5dmixyxaH4SQSXAJh.I.8PeJJ5wNznWLs0vEPa8RjD4sbfIUIMK', '李晓雨', '13800000003', 0, 1, NOW() - INTERVAL 90 DAY),
(4, 'user3', '$2a$10$ih5dmixyxaH4SQSXAJh.I.8PeJJ5wNznWLs0vEPa8RjD4sbfIUIMK', '王嘉乐', '13800000004', 0, 1, NOW() - INTERVAL 60 DAY),
(5, 'user4', '$2a$10$ih5dmixyxaH4SQSXAJh.I.8PeJJ5wNznWLs0vEPa8RjD4sbfIUIMK', '赵宁', '13800000005', 0, 0, NOW() - INTERVAL 30 DAY);

INSERT INTO room (id, room_name, room_type, room_number, price, bed_type, max_people, description, status, is_promotion, promotion_price, create_time) VALUES
(1, '雅致大床房', '大床房', '0301', 299.00, '1.8米大床', 2, '城市景观、独立卫浴、含双早', 1, 1, 259.00, NOW() - INTERVAL 150 DAY),
(2, '豪华双床房', '双床房', '0302', 399.00, '1.2米双床', 2, '高楼层城市景观、办公桌', 2, 0, NULL, NOW() - INTERVAL 150 DAY),
(3, '行政大床房', '大床房', '0501', 529.00, '2米大床', 2, '行政楼层、迷你吧、延迟退房', 3, 0, NULL, NOW() - INTERVAL 140 DAY),
(4, '家庭亲子房', '家庭房', '0401', 599.00, '大床+单人床', 3, '儿童主题布置、亲子用品', 1, 1, 539.00, NOW() - INTERVAL 130 DAY),
(5, '商务套房', '套房', '0601', 899.00, '2米大床', 2, '独立客厅、商务会客区', 1, 0, NULL, NOW() - INTERVAL 120 DAY),
(6, '景观双床房', '双床房', '0502', 459.00, '1.35米双床', 2, '落地窗、城市夜景', 2, 1, 419.00, NOW() - INTERVAL 110 DAY),
(7, '舒适大床房', '大床房', '0201', 239.00, '1.8米大床', 2, '安静楼层、基础设施齐全', 1, 0, NULL, NOW() - INTERVAL 100 DAY),
(8, '舒适双床房', '双床房', '0202', 269.00, '1.2米双床', 2, '适合朋友及商务出行', 1, 0, NULL, NOW() - INTERVAL 90 DAY),
(9, '行政套房', '套房', '0701', 1199.00, '2米大床', 2, '行政酒廊权益、独立客厅', 1, 1, 999.00, NOW() - INTERVAL 80 DAY),
(10, '无障碍客房', '大床房', '0101', 289.00, '1.8米大床', 2, '无障碍通道及卫浴设施', 1, 0, NULL, NOW() - INTERVAL 70 DAY),
(11, '家庭套房', '家庭房', '0602', 1099.00, '大床+双床', 4, '两室一厅，适合家庭入住', 1, 0, NULL, NOW() - INTERVAL 60 DAY),
(12, '维修测试房', '大床房', '0901', 199.00, '1.5米大床', 2, '演示维护状态，不对外预订', 4, 0, NULL, NOW() - INTERVAL 50 DAY);

INSERT INTO booking (id, order_no, user_id, room_id, room_name, room_number, check_in_date, check_out_date, days, price, total_amount, contact_name, contact_phone, guest_name, guest_id_card, deposit, actual_check_in_time, actual_check_out_time, deposit_return, additional_charges, remark, status, create_time) VALUES
(1, 'DEMO-PENDING-001', 2, 1, '雅致大床房', '0301', CURDATE() + INTERVAL 10 DAY, CURDATE() + INTERVAL 12 DAY, 2, 259.00, 518.00, '张晨', '13800000002', NULL, NULL, 0.00, NULL, NULL, 0.00, 0.00, '等待酒店确认', 0, NOW() - INTERVAL 2 HOUR),
(2, 'DEMO-CONFIRMED-001', 3, 2, '豪华双床房', '0302', CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 8 DAY, 3, 399.00, 1197.00, '李晓雨', '13800000003', NULL, NULL, 0.00, NULL, NULL, 0.00, 0.00, '已确认，等待入住', 1, NOW() - INTERVAL 1 DAY),
(3, 'DEMO-CHECKEDIN-001', 4, 3, '行政大床房', '0501', CURDATE(), CURDATE() + INTERVAL 2 DAY, 2, 529.00, 1058.00, '王嘉乐', '13800000004', '王嘉乐', '110101199001011234', 300.00, NOW() - INTERVAL 3 HOUR, NULL, 0.00, 0.00, '在住订单', 3, NOW() - INTERVAL 3 DAY),
(4, 'DEMO-CHECKEDOUT-001', 2, 4, '家庭亲子房', '0401', CURDATE() - INTERVAL 3 DAY, CURDATE() - INTERVAL 1 DAY, 2, 539.00, 1078.00, '张晨', '13800000002', '张晨', '110101199202023456', 300.00, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 HOUR, 260.00, 40.00, '迷你吧消费40元，今日完成退房', 4, NOW() - INTERVAL 8 DAY),
(5, 'DEMO-CANCELLED-001', 3, 5, '商务套房', '0601', CURDATE() + INTERVAL 15 DAY, CURDATE() + INTERVAL 16 DAY, 1, 899.00, 899.00, '李晓雨', '13800000003', NULL, NULL, 0.00, NULL, NULL, 0.00, 0.00, '用户主动取消', 2, NOW() - INTERVAL 5 DAY),
(6, 'DEMO-CONFIRMED-002', 2, 6, '景观双床房', '0502', CURDATE() + INTERVAL 20 DAY, CURDATE() + INTERVAL 22 DAY, 2, 419.00, 838.00, '张晨', '13800000002', NULL, NULL, 0.00, NULL, NULL, 0.00, 0.00, '第二个已确认订单', 1, NOW() - INTERVAL 6 DAY),
(7, 'DEMO-CHECKEDOUT-002', 4, 7, '舒适大床房', '0201', CURDATE() - INTERVAL 10 DAY, CURDATE() - INTERVAL 8 DAY, 2, 239.00, 478.00, '王嘉乐', '13800000004', '王嘉乐', '110101199001011234', 200.00, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 8 DAY, 200.00, 0.00, '历史已退房订单', 4, NOW() - INTERVAL 15 DAY);

INSERT INTO housekeeping_task
    (id, task_no, booking_id, room_id, room_number, task_type, status, priority, due_time, create_time) VALUES
(1, 'DEMO-HK-ROOM-0401', 4, 4, '0401', 'CHECKOUT_CLEANING', 0, 2,
 NOW() - INTERVAL 30 MINUTE, NOW() - INTERVAL 1 HOUR);

UPDATE room SET status = 5 WHERE id = 4;

INSERT INTO operation_reminder
    (id, reminder_key, reminder_type, level, title, content, booking_id, room_id, task_id, status, trigger_time, create_time) VALUES
(1, 'CLEANING_OVERDUE:1', 'CLEANING_OVERDUE', 2, '退房清洁已逾期',
 '房间 0401 清洁任务已逾期 30 分钟', 4, 4, 1, 0, NOW() - INTERVAL 30 MINUTE, NOW());

INSERT INTO notice (id, title, content, status, create_time) VALUES
(1, '欢迎体验智慧酒店管理系统', '本系统数据均为本地演示数据，可用于预订、入住、退房和权限流程演示。', 1, NOW() - INTERVAL 10 DAY),
(2, '暑期促销活动', '雅致大床房、家庭亲子房和行政套房正在进行限时促销。', 1, NOW() - INTERVAL 7 DAY),
(3, '入住证件提醒', '办理入住时请准备有效身份证件，并配合前台完成实名登记。', 1, NOW() - INTERVAL 3 DAY),
(4, '停车服务说明', '住店客人可在前台登记车牌后免费停车。', 1, NOW() - INTERVAL 1 DAY),
(5, '内部维护公告', '该公告用于演示隐藏状态，前台不展示。', 0, NOW());

INSERT INTO feedback (id, user_id, title, content, type, status, reply, create_time) VALUES
(1, 2, '房间整洁舒适', '房间卫生很好，前台办理速度也很快。', 2, 1, '感谢您的认可，期待再次入住！', NOW() - INTERVAL 6 DAY),
(2, 3, '建议增加充电设备', '希望客房内增加更多USB和Type-C充电接口。', 0, 0, NULL, NOW() - INTERVAL 4 DAY),
(3, 4, '早餐品类建议', '建议增加本地特色早餐和低糖食品。', 0, 1, '建议已转交餐饮部门，感谢您的反馈。', NOW() - INTERVAL 3 DAY),
(4, 2, '夜间噪声问题', '走廊夜间有短时噪声，希望加强巡查。', 1, 0, NULL, NOW() - INTERVAL 2 DAY),
(5, 3, '前台服务表扬', '前台工作人员耐心协助调整了入住安排。', 2, 1, '感谢您的表扬，我们会继续保持。', NOW() - INTERVAL 1 DAY);

SET FOREIGN_KEY_CHECKS = 1;
