# Mystyle

智能个人作品集系统：高级个人展示、经历资产库、工作模块复现实验室、JD 智能适配、面试模式与 CI/CD 部署样板。

## Project Structure

```text
apps
  web      Next.js frontend
  server   Spring Boot backend
infra
  docker
  nginx
  jenkins
docs
```

## Project Overview

完整项目规划、已实现功能、后续待对接事项和上手路径见：

- [docs/15-project-overview-and-roadmap.md](docs/15-project-overview-and-roadmap.md)

## Local Commands

```bash
npm run dev:web
npm run dev:server
npm run docker:up
```

## Docker Full Stack

推荐用 Docker 作为本地联调和面试展示入口，避免 Next.js dev server 因端口占用自动切换到 3001、3002。

```bash
cp .env.example .env
npm run docker:up
```

启动后只访问一个固定入口：

```text
http://localhost:3000
```

完整 Docker 拓扑：

```text
Browser -> nginx:3000 -> web:3000
                    \-> server:8080 -> mysql:3306
                                      -> redis:6379
```

常用命令：

```bash
npm run docker:down
docker compose -f infra/docker/docker-compose.local.yml logs -f nginx web server
docker compose -f infra/docker/docker-compose.local.yml ps
```

如果本机 `3000` 已被旧的 `next dev` 占用，Docker 会明确启动失败，而不会自动漂移端口。此时可以先关闭旧前端进程，或者在 `.env` 中临时设置：

```bash
APP_HTTP_PORT=3100
```
