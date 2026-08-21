# 天幕后端生产部署

后端以 Java 17 JAR 运行，中间件可以运行在同一台 Linux 服务器或受保护的内网主机上。

## 构建

```bash
cd backend
./mvnw clean package
```

构建产物位于 `target/`，不要提交到 Git。部署时只需上传生成的 JAR、数据库初始化脚本（首次部署）和服务器本地的真实环境文件。

## 配置

在服务器复制模板并填写真实值：

```bash
cp prod.env.example prod.env
chmod 600 prod.env
```

重点检查：

- MySQL、Redis、Elasticsearch、MinIO、RocketMQ 和 Canal 的连接信息
- `TIANMU_MINIO_PUBLIC_URL` 是浏览器能够访问的 HTTPS 地址
- `TIANMU_CORS_ALLOWED_ORIGIN_PATTERNS` 只包含实际前端域名
- `TIANMU_JWT_SECRET` 是 Base64 编码的至少 32 字节随机密钥
- 邮箱使用 SMTP 授权码，不使用邮箱登录密码

生成 JWT 密钥的示例：

```bash
openssl rand -base64 32
```

## 启动

```bash
set -a
source ./prod.env
set +a
java -jar ./tianmu.jar
```

确认前台启动正常后，再交由 systemd、容器编排平台或其他进程管理工具托管。

默认端口：

- HTTP API：`8101`，统一前缀 `/api`
- 弹幕 WebSocket：`9101`

## 网络安全

生产环境建议只通过 Nginx 或网关开放 `80/443`。MySQL `3306`、Redis `6379`、Elasticsearch `9200/9300`、RocketMQ `9876`、Canal `11111` 等端口不应直接暴露到公网。

真实的 `prod.env`、云服务器 IP、密码、密钥和 Token 都不得提交到仓库。
