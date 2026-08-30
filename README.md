# 智慧酒店管理系统

基于 Spring Boot 2.7、Spring Security、MyBatis-Plus、MySQL 8 和原生 HTML/CSS/JavaScript 的酒店业务系统，使用 JDK 21 构建。

项目除基础的登录、客房、预订、入住和退房外，重点实现了并发防超卖与幂等预订、可解释智能定价、退房清洁任务和自动运营提醒。

## 从零启动

1. 安装 JDK 21、Maven 3.9+ 和 MySQL 8。
2. 按顺序执行 `database/migrations/V1__baseline_schema.sql`、V2、V3、V4、V5。
3. 执行 `database/demo-data.sql` 填充演示数据。该脚本会清空并重建 8 张业务表的数据，不要用于需要保留数据的数据库。
4. 如本地数据库账号不是 `root/root`，设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量。
5. 构建并启动：

```powershell
$env:JAVA_HOME = "E:\jdk\jdk21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
.\mvnw.cmd clean package
& "$env:JAVA_HOME\bin\java.exe" -jar target\demo4-1.0.0.jar
```

访问入口：

- 用户登录：`http://localhost:8080/pages/user/login.html`
- 管理员登录：`http://localhost:8080/pages/admin/admin-login.html`
- 管理员：`admin / admin123`
- 普通用户：`user1 / 123456`

部署环境必须通过 `AUTH_TOKEN_SECRET` 设置足够长的随机 JWT 密钥，不要使用本地默认值。

## 验证

```powershell
.\mvnw.cmd clean package
```

当前 JDK 21 全量构建包含 43 项自动化测试。功能设计、迁移和真实回归证据见：

- `docs/BOOKING_CONCURRENCY.md`
- `docs/SMART_PRICING.md`
- `docs/OPERATIONS_AUTOMATION.md`
- `docs/BUSINESS_MONITORING_DASHBOARD.md`
- `docs/PERFORMANCE_TEST.md`
- `docs/RESUME_ACHIEVEMENTS.md`
