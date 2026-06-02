# 智能个人作品集系统 - 详细设计阶段记录

## 1. 当前落地范围

本阶段先完成项目骨架，不直接实现完整业务。

已规划目录：

```text
apps/web      Next.js 前端
apps/server   Spring Boot 后端
infra/docker  Docker Compose
infra/nginx   Nginx 配置
infra/jenkins Jenkinsfile
docs          软件工程文档
```

## 2. 前端详细设计初版

### 2.1 页面层

- `/` 首页
- `/resume` 在线简历，待实现
- `/projects` 项目列表，待实现
- `/projects/[slug]` 项目详情，待实现
- `/lab` 模块实验室，待实现
- `/lab/[slug]` 模块详情，待实现
- `/jd-adapter` JD 智能适配器，待实现
- `/interview` 面试模式，待实现
- `/admin` 后台管理，待实现

### 2.2 数据策略

第一阶段：

- 使用 `apps/web/lib/data.ts` 作为前端 mock 数据。
- 后端提供 `/api/public/home` mock 接口。

第二阶段：

- 前端通过 API Client 读取后端数据。
- 后端接入 MySQL 和 Redis。

## 3. 后端详细设计初版

### 3.1 已创建模块

- `common`：统一响应、异常处理。
- `health`：健康检查。
- `profile`：公开首页数据。
- `jd`：JD 分析接口 mock。
- `moduleDemo`：视频学习 Demo mock。

### 3.2 后续模块

- `auth`
- `project`
- `experience`
- `skill`
- `variant`
- `share`
- `analytics`
- `article`

## 4. 当前接口

```text
GET  /api/health
GET  /api/public/home
GET  /api/public/resume
GET  /api/public/interview
GET  /api/public/projects
GET  /api/public/projects/{slug}
GET  /api/public/module-demos
GET  /api/public/module-demos/{slug}
POST /api/jd/analyze
POST /api/lab/video-learning/reset
POST /api/lab/video-learning/progress
POST /api/lab/video-learning/complete
GET  /api/lab/video-learning/snapshot
```

## 5. 后续详细设计任务

- 将数据库设计转换为 migration。
- 设计 DTO、VO、Entity、Mapper 分层。
- 设计前端 API Client。
- 设计 ModuleDemo Renderer 注册表。
- 设计 JD Prompt 模板和 Provider 抽象。
- 设计后台管理表单结构。
