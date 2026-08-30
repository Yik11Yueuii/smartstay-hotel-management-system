-- 在现有演示库中幂等补充经营大屏数据，不清空任何业务表。
USE hotel_management;
SET NAMES utf8mb4;

UPDATE booking SET base_price = 259.00, price = 246.05, total_amount = 492.10,
    pricing_strategy_version = 'SMART_PRICING_V1',
    pricing_snapshot = JSON_OBJECT('strategyVersion', 'SMART_PRICING_V1', 'decision', '提前预订优惠')
WHERE order_no = 'DEMO-PENDING-001';
UPDATE booking SET base_price = 399.00, price = 438.90, total_amount = 1316.70,
    pricing_strategy_version = 'SMART_PRICING_V1',
    pricing_snapshot = JSON_OBJECT('strategyVersion', 'SMART_PRICING_V1', 'decision', '高入住率与周末溢价')
WHERE order_no = 'DEMO-CONFIRMED-001';
UPDATE booking SET base_price = 419.00, price = 385.48, total_amount = 770.96,
    pricing_strategy_version = 'SMART_PRICING_V1',
    pricing_snapshot = JSON_OBJECT('strategyVersion', 'SMART_PRICING_V1', 'decision', '长提前期优惠')
WHERE order_no = 'DEMO-CONFIRMED-002';

INSERT INTO business_metric_event(event_type, room_id, user_id, booking_id, detail, create_time)
SELECT 'INVENTORY_CONFLICT', 2, 8, NULL, '[DEMO-METRIC-01] 同房同日期库存冲突已拦截', NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM business_metric_event WHERE detail LIKE '[DEMO-METRIC-01]%');
INSERT INTO business_metric_event(event_type, room_id, user_id, booking_id, detail, create_time)
SELECT 'INVENTORY_CONFLICT', 2, 9, NULL, '[DEMO-METRIC-02] 同房同日期库存冲突已拦截', NOW() - INTERVAL 6 DAY
WHERE NOT EXISTS (SELECT 1 FROM business_metric_event WHERE detail LIKE '[DEMO-METRIC-02]%');
INSERT INTO business_metric_event(event_type, room_id, user_id, booking_id, detail, create_time)
SELECT 'IDEMPOTENT_REPLAY', 1, 2, 1, '[DEMO-METRIC-03] 网络重试返回原订单', NOW() - INTERVAL 5 DAY
WHERE NOT EXISTS (SELECT 1 FROM business_metric_event WHERE detail LIKE '[DEMO-METRIC-03]%');
INSERT INTO business_metric_event(event_type, room_id, user_id, booking_id, detail, create_time)
SELECT 'INVENTORY_CONFLICT', 3, 10, NULL, '[DEMO-METRIC-04] 不可售房态请求已拦截', NOW() - INTERVAL 3 DAY
WHERE NOT EXISTS (SELECT 1 FROM business_metric_event WHERE detail LIKE '[DEMO-METRIC-04]%');
INSERT INTO business_metric_event(event_type, room_id, user_id, booking_id, detail, create_time)
SELECT 'IDEMPOTENCY_KEY_CONFLICT', 6, 2, 6, '[DEMO-METRIC-05] 同一幂等键用于不同请求', NOW() - INTERVAL 2 DAY
WHERE NOT EXISTS (SELECT 1 FROM business_metric_event WHERE detail LIKE '[DEMO-METRIC-05]%');
INSERT INTO business_metric_event(event_type, room_id, user_id, booking_id, detail, create_time)
SELECT 'IDEMPOTENT_REPLAY', 6, 2, 6, '[DEMO-METRIC-06] 网络重试返回原订单', NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (SELECT 1 FROM business_metric_event WHERE detail LIKE '[DEMO-METRIC-06]%');
INSERT INTO business_metric_event(event_type, room_id, user_id, booking_id, detail, create_time)
SELECT 'INVENTORY_CONFLICT', 3, 11, NULL, '[DEMO-METRIC-07] 不可售房态请求已拦截', NOW() - INTERVAL 4 HOUR
WHERE NOT EXISTS (SELECT 1 FROM business_metric_event WHERE detail LIKE '[DEMO-METRIC-07]%');
