# 2025 历史演示数据

`database/2025-history-data.sql` 用于补充与“系统于 2025 年 12 月完成”相符的历史经营样本。数据均为模拟数据，不应描述为真实生产数据。

## 数据构成

- 70 条订单，入住日期从 2025-01-15 延续至 2025-12-26；
- 63 条已退房订单、7 条取消订单；
- 淡季、暑期、国庆、十二月和周末使用不同价格系数；
- 十二月订单保存 `SMART_PRICING_V1` 定价快照；
- 16 条已完成退房清洁任务，保留完整处理时间；
- 15 条已处理客户反馈，覆盖表扬、投诉和产品建议。

脚本按 `HIST-2025-*`、`HIST-HK-2025-*` 和 `[2025历史]` 前缀判断数据是否存在，可安全重复执行，不会重置当前演示数据。

## 导入与回滚

```bash
mysql -uroot -proot --default-character-set=utf8mb4 -D hotel_management -e "source database/2025-history-data.sql"
```

如需只删除这批历史样本：

```bash
mysql -uroot -proot --default-character-set=utf8mb4 -D hotel_management -e "source database/2025-history-data-rollback.sql"
```

## 演示口径

可以说明系统通过历史订单支撑入住率、取消率、经营收入和定价效果分析；不要把模拟数据的金额或指标包装成线上生产成果。

