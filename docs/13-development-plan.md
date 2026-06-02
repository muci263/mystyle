# 智能个人作品集系统 - 迭代开发计划

## 1. 开发原则

- 先完成可上线闭环，再逐步增强。
- 每个阶段都有可演示成果。
- 前端高级感通过真实页面不断迭代，不依赖一次性设计稿。
- 后端围绕 Java 秋招重点能力展开。
- 文档、代码、部署同步推进。

## 2. 阶段一：项目骨架与公开展示闭环

目标：

- 建立 Monorepo。
- 完成前端基础视觉和公开页面。
- 后端提供基础公开接口。
- 使用结构化数据展示简历和项目。

任务：

1. 创建目录结构。
2. 初始化 Next.js 前端。
3. 初始化 Spring Boot 后端。
4. 设计基础 UI 风格。
5. 实现首页 V1。
6. 实现在线简历页。
7. 实现项目列表和详情页。
8. 后端提供 Profile、Project、Experience 基础接口。
9. 准备种子数据。

阶段成果：

- 本地可访问作品集首页。
- 内容来自结构化数据。
- 至少一个项目详情页完整可读。

## 3. 阶段二：工作模块复现实验室

目标：

- 实现模块注册机制。
- 完成视频播放学习链路 Demo。

任务：

1. 实现 ModuleDemo 数据结构。
2. 实现 Lab 首页。
3. 实现 DemoRenderer 分发。
4. 实现 video-learning 页面。
5. 后端实现视频进度上报、快照、完播、重置接口。
6. 前端展示 Redis/MySQL/状态机模拟快照。

阶段成果：

- 面试官可以实际操作视频学习链路 Demo。
- 项目经历与 Demo 关联。

## 4. 阶段三：JD 智能适配与定制分享

目标：

- 输入 JD 后生成岗位适配结果。
- 保存 ProfileVariant。
- 通过分享链接展示不同内容。

任务：

1. 实现 LLM Provider 抽象。
2. 编写 JD 分析 Prompt。
3. 实现 `/api/jd/analyze`。
4. 实现 ProfileVariant。
5. 前端实现 JD Adapter 页面。
6. 首页和项目页支持 variantId。
7. 实现 ShareLink。

阶段成果：

- 输入 Java 后端 JD 后生成项目排序和个人介绍。
- 分享链接可展示对应版本。

## 5. 阶段四：后台管理与认证

目标：

- 管理员可维护内容。
- 后台接口受保护。

任务：

1. 实现用户登录。
2. 实现 JWT 鉴权。
3. 实现后台布局。
4. 实现 Profile 管理。
5. 实现 Experience 管理。
6. 实现 Project 管理。
7. 实现 ModuleDemo 管理。
8. 实现 Variant 和 ShareLink 管理。

阶段成果：

- 不改代码也能维护主要内容。

## 6. 阶段五：工程化部署

目标：

- 完成 Docker Compose 和 Jenkins 自动部署。

任务：

1. 编写前端 Dockerfile。
2. 编写后端 Dockerfile。
3. 编写 docker-compose。
4. 编写 nginx.conf。
5. 编写 Jenkinsfile。
6. 配置服务器环境。
7. 配置 GitHub Webhook。
8. 完成健康检查。

阶段成果：

- GitHub push 后可自动部署。
- 网站真实上线。

## 7. 阶段六：增强功能

目标：

- 提升面试展示力和工程深度。

任务：

1. 项目证据链。
2. 面试模式。
3. Agent 工作流 Demo。
4. 技术能力地图。
5. 架构交互图。
6. 访问统计。
7. 项目复盘文章。

## 8. 第一版 MVP 范围

必须包含：

- 首页
- 在线简历
- 项目详情
- 模块实验室骨架
- 视频学习 Demo
- JD 适配基础版
- 结构化数据
- Docker Compose

可以后置：

- 完整后台管理
- Jenkins 自动部署
- Agent 真实工具调用
- PDF 导出
- 复杂访问分析

## 9. 建议开发顺序

```text
docs
  -> frontend skeleton
  -> backend skeleton
  -> seed data
  -> public pages
  -> lab demo
  -> jd adapter
  -> auth/admin
  -> docker
  -> jenkins
```
