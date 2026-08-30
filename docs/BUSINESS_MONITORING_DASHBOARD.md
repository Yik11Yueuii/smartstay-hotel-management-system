# 经营决策与监控大屏

## 设计原则

大屏不以“图越多越好”为目标。每个指标必须回答一个使用者问题，并能对应下一步动作：

| 使用者问题 | 指标/图表 | 决策动作 |
| --- | --- | --- |
| 哪些日期库存紧张或空置？ | 未来 14 天逐日预测入住率 | 高于 80% 检查促销并评估提价；低于 30% 评估定向促销 |
| 智能定价究竟带来增收还是让利？ | 近 30 天基础价金额与智能预订金额对比 | 调整溢价/优惠规则，检查定价覆盖率 |
| 防超卖和幂等是否真的在工作？ | 近 7 天库存冲突、幂等键冲突和安全重放趋势 | 判断异常流量、验证保护机制、排查客户端重试 |
| 现在最需要优化哪个接口？ | 最近 15 分钟 P50/P95/P99、5xx 错误率和最慢接口榜 | 按 P95 和 5xx 错误率定位性能治理对象 |
| 哪些问题应先处理？ | 基于阈值生成的行动建议 | 直接跳转客房、订单或运营中心处理 |

页面入口：

    http://localhost:8080/pages/admin/admin-monitoring.html

接口 GET /api/dashboard/decision 仅管理员可访问。

## 指标口径

### 入住率预测

- 时间范围：今天起 14 天。
- 分母：非维护状态客房数；待清洁房仍属于酒店容量，但会在“库存就绪”中单独展示。
- 分子：待确认、已确认、已入住订单在指定日期占用的去重客房数。
- 同一天同一房间最多计算一次，避免异常重复订单放大入住率。
- 柱状图阈值：≤30% 标记促销机会，≥80% 标记提价评估日。

### 智能定价效果

- 时间范围：近 30 天创建且未取消的订单。
- 仅统计同时存在 pricing_strategy_version 和 base_price 的订单。
- 基础价金额：base_price × days。
- 智能预订金额：订单服务端最终保存的 total_amount。
- 净影响：智能预订金额减基础价金额。正值代表溢价金额，负值代表策略让利，不等同于已确认财务收入。
- 定价覆盖率：带可信定价快照的有效订单数 ÷ 近 30 天有效订单数。

### 预订保护事件

V5 新增 business_metric_event 表，记录三类事件：

- INVENTORY_CONFLICT：不可售房态或日期库存冲突被拦截。
- IDEMPOTENT_REPLAY：客户端重放同一请求，系统安全返回原订单。
- IDEMPOTENCY_KEY_CONFLICT：同一幂等键被用于不同预订请求。

事件先进入最多 5000 条的内存队列，大屏合并已落库和待落库事件；后台默认每 5 秒批量写入数据库。这样不会在持有客房行锁的预订事务中额外占用连接，也不会因为监控写入失败破坏预订主流程。

### 接口性能

- 采样范围：当前应用实例的 /api/** 请求。
- 默认窗口：最近 15 分钟，最多 5000 个样本。
- 资源 ID 会归一化，例如 /api/booking/101 与 /api/booking/202 合并为 /api/booking/{id}，避免指标维度无限增长。
- 错误率只统计 HTTP 5xx；业务冲突 409 在预订保护区单独统计，不把正常拒绝误判为服务故障。
- 默认告警：P95 超过 500 ms 或 5xx 错误率超过 1%。
- 该窗口随应用重启重置，定位是轻量本地运行监控，不宣称替代 Prometheus、Grafana 或生产 APM。

## 数据库升级

执行 V5：

    mysql -uroot -proot --default-character-set=utf8mb4 -D hotel_management -e "source database/migrations/V5__business_monitoring_metrics.sql"

为现有演示库幂等补数据，不清空其他业务表：

    mysql -uroot -proot --default-character-set=utf8mb4 -D hotel_management -e "source database/monitoring-demo-data.sql"

回滚前确认不再需要保护事件历史，再执行 U5__business_monitoring_metrics_rollback.sql。

## 可配置阈值

    $env:MONITORING_API_MAX_SAMPLES = "5000"
    $env:MONITORING_API_WINDOW_MINUTES = "15"
    $env:MONITORING_API_SLOW_THRESHOLD_MS = "500"
    $env:MONITORING_API_ERROR_RATE_THRESHOLD = "0.01"
    $env:MONITORING_BUSINESS_METRICS_FLUSH_DELAY_MS = "5000"

## 验证证据

- JDK 21 全量构建：43 项测试，0 失败。
- 单元测试验证接口路径归一化、P50/P95、5xx 错误率、无样本状态，以及入住率、定价影响、保护事件和运营风险的聚合口径。
- 真实 MySQL/API 回归：测试预订返回 409，大屏库存冲突从 4 增至 5，事件随后批量落库；测试事件按 ID 精确清理，未生成意外订单。
- 同一回归窗口采集 24 个接口样本，P95 为 109 ms、健康状态为 HEALTHY。该数据仅用于验证监控链路，不作为新的正式性能基线。

## 当前边界

- 入住率来自本系统订单，不包含 OTA、线下渠道、临时锁房和 no-show 预测。
- 定价净影响是相对基础价的订单金额差，不是因果实验结论；若要评估策略真实增量，应增加 A/B 分组和渠道成本。
- 多实例部署时，每个实例拥有独立接口性能窗口；生产环境应接入 Micrometer/Prometheus 并按实例聚合。
- 业务事件采用本地队列批量落库，极端进程崩溃可能丢失尚未刷新的少量监控事件，不影响核心订单一致性。
