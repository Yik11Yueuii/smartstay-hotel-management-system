# 退房清洁与自动运营提醒

## 解决的问题

原流程在办理退房后会立即释放客房，保洁工作只能靠线下口头通知。这样既可能把尚未清洁的房间重新售出，也无法及时发现清洁逾期和临近入住风险。

当前实现把退房后的客房流转做成可追踪闭环：

```text
办理退房
  -> 发布 BookingCheckedOutEvent
  -> 幂等生成退房清洁任务
  -> 房态切换为 5（待清洁，不可售）
  -> 定时扫描清洁逾期/临近入住风险
  -> 管理员开始并完成清洁
  -> 发布 HousekeepingCompletedEvent
  -> 自动关闭关联提醒
  -> 按现有订单恢复为可售、已预订或已入住
```

这里使用的是 Spring 进程内领域事件和同一数据库事务，不是 Kafka/RabbitMQ。`BEFORE_COMMIT` 监听保证订单退房、清洁任务和房态更新要么一起提交，要么一起回滚。

## 一致性与幂等

- `housekeeping_task` 以 `(booking_id, task_type)` 建立唯一约束，同一退房事件重复执行不会生成多条清洁任务。
- `operation_reminder` 以 `reminder_key` 建立唯一约束，定时任务和人工扫描并发执行时不会重复提醒。
- 清洁任务仅允许 `待处理 -> 清洁中 -> 已完成`，非法重复操作返回业务冲突。
- 房态 `5` 表示待清洁；定价库存和用户可订列表均排除维护中及待清洁房间。
- 完成最后一个活动清洁任务后，系统检查该房间现有订单，再恢复为可售、已预订或已入住，避免错误释放库存。

## 自动提醒规则

1. 清洁任务超过 `due_time` 后生成逾期提醒；逾期不足 30 分钟为普通提醒，达到 30 分钟升级为紧急。
2. 房间仍有活动清洁任务，且存在今天或明天入住的已确认订单时，生成临近入住风险提醒。
3. 清洁完成事件自动关闭该任务关联的全部待处理提醒。
4. 默认启动 30 秒后开始扫描，此后每 60 秒扫描一次，可通过环境变量调整：

```powershell
$env:OPERATIONS_REMINDER_ENABLED = "true"
$env:OPERATIONS_REMINDER_INITIAL_DELAY_MS = "30000"
$env:OPERATIONS_REMINDER_SCAN_DELAY_MS = "60000"
```

## 管理端入口与接口

管理员登录后访问：

```text
http://localhost:8080/pages/admin/admin-operations.html
```

运营中心展示待清洁、清洁中、已逾期和待处理提醒，支持分配负责人、开始清洁、完成清洁和立即扫描风险。`/api/operations/**` 仅管理员角色可访问。

主要接口：

- `GET /api/operations/overview`
- `PUT /api/operations/tasks/{taskId}/start`
- `PUT /api/operations/tasks/{taskId}/complete`
- `POST /api/operations/reminders/scan`

## 数据库升级与演示数据

执行 V4 升级：

```powershell
mysql -uroot -proot --default-character-set=utf8mb4 -D hotel_management -e "source database/migrations/V4__operations_housekeeping_reminders.sql"
```

在已有演示库中补一条可重复执行的运营任务：

```powershell
mysql -uroot -proot --default-character-set=utf8mb4 -D hotel_management -e "source database/operations-demo-data.sql"
```

需要回滚时先确认不再需要历史清洁任务和提醒，再执行 `U4__operations_housekeeping_reminders_rollback.sql`。

## 验证证据

- JDK 21 全量构建：43 项测试，0 失败。
- H2 集成测试覆盖退房生成任务、房态隔离、重复扫描幂等、任务状态迁移、提醒自动关闭和房态恢复。
- 真实 MySQL/API 回归：退房后订单状态为 4、房态为 5、任务状态为 0；逾期扫描两次分别新增 1/0 条提醒；完成清洁后任务状态为 2、提醒状态为 1、房态恢复为 1。
- 真实回归使用 `OPS-VERIFY-0830` 隔离数据，验证完成后订单、客房、任务和提醒均已精确清理。

## 当前边界

- 当前事件仅在单体应用进程和同一事务内传递；若未来拆分服务，应使用事务消息或 Outbox 保证跨进程可靠投递。
- 当前调度器适合单实例部署；多实例部署应增加分布式调度锁，避免重复扫描带来的无效数据库竞争。
- 当前负责人为文本字段；下一步可接入员工、班次、移动端接单和清洁质检记录。
