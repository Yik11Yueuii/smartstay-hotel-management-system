-- 仅回滚 database/2025-history-data.sql 生成的数据。
SET NAMES utf8mb4;

DELETE FROM housekeeping_task WHERE task_no LIKE 'HIST-HK-2025-%';
DELETE FROM feedback WHERE title LIKE '[2025历史] #%';
DELETE FROM booking WHERE order_no LIKE 'HIST-2025-%';

