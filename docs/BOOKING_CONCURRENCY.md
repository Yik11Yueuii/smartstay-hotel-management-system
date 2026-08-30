# 预订幂等与防超卖

## 解决的问题

原创建流程是“查询日期冲突 → 插入订单”。两个事务可能同时查询到无冲突并各自插入，造成同一客房日期重叠；浏览器重复点击或网络重试也可能创建重复订单。

## 实现

1. 前端每次预订生成 16–64 位 `Idempotency-Key`，网络失败重试时复用该键。
2. 后端先按 `(user_id, idempotency_key)` 查询历史结果；相同请求直接返回原订单，不重复扣减或插入。
3. 创建事务通过 `SELECT ... FOR UPDATE` 锁定目标客房行，再检查有效订单的日期区间并插入订单。
4. 数据库唯一约束 `uk_booking_user_idempotency` 作为幂等最终兜底，联合索引 `idx_booking_room_dates` 支持冲突查询。
5. 同一幂等键若被用于不同房间或日期，返回 HTTP 409 `STATE_CONFLICT`。

客房行锁由 MySQL/InnoDB 管理，因此应用部署多个实例时仍共享同一并发边界，不依赖 JVM 本地锁。

## 数据库迁移

```powershell
mysql -uroot -p -D hotel_management -e "source database/migrations/V2__booking_idempotency.sql"
```

回滚脚本为 `database/migrations/U2__booking_idempotency_rollback.sql`。回滚前应先停止仍会写入 `Idempotency-Key` 的应用版本。

## 验证证据

- 重放同一用户、同一请求键：两次响应返回同一个订单 ID，数据库只有一条记录。
- 20 个线程使用不同用户和请求键竞争同一客房、同一日期：1 个成功，19 个返回 `STATE_CONFLICT`，数据库只有一条有效订单。
- 真实 MySQL API 回归：首次和重放请求均返回订单 ID 8，测试数据随后按 ID、用户、幂等键和备注精确清理，剩余 0 条。
