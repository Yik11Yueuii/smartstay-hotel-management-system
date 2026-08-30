-- 保存下单时的智能定价依据，确保订单价格可解释、可审计。
ALTER TABLE booking
    ADD COLUMN base_price DECIMAL(10,2) NULL COMMENT '下单时基础间夜价' AFTER days,
    ADD COLUMN pricing_strategy_version VARCHAR(50) NULL COMMENT '定价策略版本' AFTER total_amount,
    ADD COLUMN pricing_snapshot JSON NULL COMMENT '逐日定价规则快照' AFTER pricing_strategy_version;

-- 历史订单缺少规则快照，以原成交单价回填基础价。
UPDATE booking SET base_price = price WHERE base_price IS NULL;
