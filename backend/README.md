# 天幕后端

基于 Java 17、Spring Boot 3 和 MyBatis-Plus 的视频社区后端。

## 开发运行

1. 准备 MySQL、Redis、Elasticsearch、MinIO、RocketMQ 和 Canal。
2. 设置 `application-local.yml` 中引用的环境变量。
3. 执行 `./mvnw spring-boot:run`，Windows 使用 `.\mvnw.cmd spring-boot:run`。

配置模板见 [`deploy/prod.env.example`](deploy/prod.env.example)，数据库初始化脚本见 [`deploy/mysql/init.sql`](deploy/mysql/init.sql)。执行 `create-local-user.sql` 前必须先替换其中的 `CHANGE_ME_LOCAL_PASSWORD`。

生产部署见 [`deploy/README-prod.md`](deploy/README-prod.md)。
