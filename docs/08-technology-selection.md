# 智能个人作品集系统 - 技术选型文档

## 1. 技术选型原则

- 贴合 Java 后端秋招方向。
- 前端具备高级展示能力。
- 后端能体现工程深度。
- 生态成熟，便于部署和维护。
- 支持后续扩展 JD 智能适配、模块复现、后台管理和 CI/CD。

## 2. 前端技术栈

### 2.1 核心框架：Next.js

选择原因：

- 支持 App Router、SSR、SSG，适合作品集展示。
- SEO 友好，公开项目页更适合被访问和分享。
- React 生态成熟，适合构建复杂交互 Demo。
- 后续可将部分页面静态化，提高访问速度。

### 2.2 语言：TypeScript

选择原因：

- 提升大型前端项目可维护性。
- 与接口类型、数据模型更容易对齐。
- 适合复杂模块和后台管理系统。

### 2.3 样式：Tailwind CSS

选择原因：

- 开发效率高。
- 适合构建自定义高级视觉。
- 不强依赖模板，可逐步打磨细节。

### 2.4 组件基础：shadcn/ui + Radix UI

选择原因：

- 组件质量高，可控性强。
- 适合后台管理、弹窗、表格、表单、命令面板。
- 代码直接进入项目，可按设计风格深度改造。

### 2.5 动效：Motion

选择原因：

- 支持页面进入、模块切换、卡片交互等细腻动效。
- 能增强高级感，但不依赖重型动画。

### 2.6 图表与流程

建议：

- Recharts：基础图表和访问统计。
- React Flow：架构图、Agent 工作流、链路可视化。
- Monaco Editor 或轻量 Code Block：接口调试和代码展示，第一期可后置。

## 3. 后端技术栈

### 3.1 核心框架：Spring Boot

选择原因：

- 与 Java 后端求职方向强匹配。
- 面试可讲价值高。
- 适合展示统一异常处理、接口设计、参数校验、鉴权、日志等工程能力。

### 3.2 持久层：MyBatis-Plus

选择原因：

- 上手快，适合 CRUD 和后台管理。
- 与简历技术栈一致。
- 可以保留手写 SQL 能力，用于复杂查询和优化展示。

### 3.3 数据库：MySQL

选择原因：

- Java 后端常用。
- 与简历项目一致。
- 适合内容管理、访问统计、分享链接等结构化数据。

### 3.4 缓存：Redis

选择原因：

- 与简历核心亮点匹配。
- 可用于热点内容缓存、JD 分析结果缓存、Demo 状态模拟。
- 可展示视频进度缓存同步模块。

### 3.5 鉴权：Spring Security + JWT

选择原因：

- Java 后端岗位常见。
- 可展示认证、授权、Token 过期、后台路由保护。
- 后续可扩展 RBAC。

### 3.6 接口文档：Springdoc OpenAPI

选择原因：

- 自动生成 Swagger 页面。
- 适合面试展示接口规范。
- 便于前后端联调。

## 4. LLM 接入方案

### 4.1 Provider 抽象

后端不直接绑定某一家模型服务，抽象为：

```text
LLMProvider
  analyzeJD()
  generateSummary()
  generateInterviewScript()
```

### 4.2 可选模型服务

- OpenAI：能力强，接口成熟。
- DeepSeek：中文场景和成本可能更友好。
- 通义千问：国内部署和访问较方便。
- 智谱：国内 LLM 生态可选项。

第一期建议保留配置化能力，具体模型通过环境变量决定。

## 5. DevOps 技术栈

### 5.1 Docker / Docker Compose

用途：

- 本地和服务器环境统一。
- 管理 web、server、mysql、redis。

### 5.2 Nginx

用途：

- 反向代理。
- HTTPS 配置。
- 静态资源缓存。
- `/api` 转发后端。

### 5.3 Jenkins

用途：

- 自动拉取代码。
- 构建前端和后端。
- 构建或更新 Docker 服务。
- 健康检查。

选择原因：

- 与用户目标一致。
- 适合展示 CI/CD 经验。
- 面试中可讲完整部署链路。

## 6. 测试工具

前端：

- ESLint
- TypeScript Check
- Vitest 或 Jest
- Playwright 后续用于关键页面 E2E

后端：

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers 后续可选

## 7. 最终推荐组合

```text
Frontend:
  Next.js + React + TypeScript + Tailwind CSS + shadcn/ui + Motion

Backend:
  Spring Boot + Spring Security + JWT + MyBatis-Plus + Springdoc OpenAPI

Data:
  MySQL + Redis

LLM:
  Provider 抽象 + 可配置模型服务

DevOps:
  Docker Compose + Nginx + Jenkins
```

## 8. 选型风险

- Next.js + Spring Boot 前后端分离会增加工程复杂度。
- Jenkins 部署初期配置成本较高。
- LLM 调用存在成本、稳定性和生成准确性风险。
- 前端高级感需要持续打磨，不能依赖模板直接完成。

应对：

- 分阶段开发，先完成本地闭环。
- CI/CD 后置到 P1。
- LLM 输出必须结构化并经过校验。
- 前端先做可用页面，再持续视觉迭代。
