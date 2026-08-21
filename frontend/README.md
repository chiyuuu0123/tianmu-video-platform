# 天幕前端

基于 Vue 3、Element Plus、Pinia 和 ArtPlayer 的视频社区前端。

## 安装与运行

```bash
npm ci
npm run serve
```

开发环境默认将 `/api` 代理到 `http://127.0.0.1:8101`。

如需覆盖地址，复制 `.env.example` 为 `.env.local` 并修改变量。

## 构建与检查

```bash
npm run lint
npm run build
```

`dist/` 是构建产物，不提交到 Git。
