# 智能个人作品集系统 - 概要设计文档

## 1. 设计目标

本系统采用前后端分离架构，目标是在保证前端展示高级感的同时，后端具备真实工程深度，包括内容管理、权限认证、工作模块复现、JD 智能适配、分享链接、访问统计和 CI/CD 自动化部署。

系统设计需要满足：

- 可扩展：后续新增经历、项目、模块 Demo、JD Variant 时不改大结构。
- 可演示：重点功能可以直接给面试官展示。
- 可上线：支持 Docker、Nginx、Jenkins 部署。
- 可维护：结构清晰，文档完整，接口规范。

## 2. 总体架构

```text
Browser
  |
  | HTTPS
  v
Nginx
  |----------------------|
  v                      v
Next.js Web App       Spring Boot API
                         |
       |-----------------|------------------|
       v                 v                  v
     MySQL             Redis           LLM Provider
```

## 3. 系统模块划分

### 3.1 前端模块

```text
apps/web
  app
    首页
    在线简历
    项目案例
    模块实验室
    JD 智能适配器
    面试模式
    技术架构页
    后台管理
  components
    基础 UI 组件
    展示组件
    Demo Renderer
    图表和流程组件
  lib
    API Client
    权限工具
    数据格式化
```

### 3.2 后端模块

```text
apps/server
  auth          认证鉴权
  profile       个人信息
  education     教育经历
  experience    实习经历
  project       项目经历
  skill         技能管理
  module-demo   工作模块复现
  jd            JD 智能适配
  variant       Profile Variant
  share         定制分享链接
  analytics     访问统计
  article       复盘文章
  system        系统配置、日志、健康检查
```

### 3.3 基础设施模块

```text
infra
  docker
  nginx
  jenkins
  mysql
  redis
  deploy
```

## 4. 核心业务流程

### 4.1 公开访问流程

```text
访客进入首页
  -> 加载公开 Profile
  -> 加载精选 Project
  -> 加载 ModuleDemo
  -> 记录匿名访问事件
```

### 4.2 JD 智能适配流程

```text
用户粘贴 JD
  -> 后端读取 Profile / Experience / Project / Skill / ModuleDemo
  -> 构造 Prompt
  -> 调用 LLM Provider
  -> 校验结构化结果
  -> 保存 JDAnalysis
  -> 可生成 ProfileVariant
  -> 前端按 Variant 渲染首页/项目页/面试模式
```

### 4.3 工作模块复现流程

```text
用户进入 Lab
  -> 选择 ModuleDemo
  -> 根据 demoType 加载对应 DemoRenderer
  -> 前端触发演示操作
  -> 后端返回模拟或真实状态
  -> 前端展示链路、日志、快照、讲解点
```

### 4.4 后台内容维护流程

```text
管理员登录
  -> JWT 鉴权
  -> 进入后台
  -> 新增/编辑经历、项目、技能、模块
  -> 保存结构化数据
  -> 公开页面自动读取最新内容
```

## 5. 分层设计

### 5.1 前端分层

- Page Layer：路由页面，负责组合模块。
- Feature Layer：业务功能组件，例如 ProjectCase、JDAdapter、InterviewMode。
- Component Layer：Button、Card、Table、Dialog 等通用组件。
- Data Layer：API Client、请求封装、状态管理。
- Renderer Layer：根据 `demoType` 渲染不同工作复现模块。

### 5.2 后端分层

- Controller：接口入口，参数校验。
- Service：业务逻辑。
- Repository / Mapper：数据库访问。
- Domain Model：核心业务对象。
- Integration：LLM、Redis、邮件等外部服务。
- Common：统一响应、异常处理、鉴权、日志。

## 6. 权限设计概览

第一版权限分为：

- Public：公开访问。
- Admin：后台管理。

后续可扩展：

- Role
- Permission
- MenuPermission
- ButtonPermission

管理端所有写操作必须鉴权，公开 Demo 接口仅允许访问演示数据。

## 7. 可扩展设计

### 7.1 内容可扩展

经历、项目、技能、模块均结构化存储，页面通过接口读取，不直接硬编码。

### 7.2 Demo 可扩展

`ModuleDemo.demoType` 决定前端渲染器。

新增模块优先复用已有 Renderer：

- video-learning
- cache-sync
- state-machine
- agent-workflow
- rag-flow
- sql-bot
- api-flow
- file-parser
- alerting
- custom

### 7.3 LLM 可扩展

后端提供 LLM Provider 抽象：

```text
LLMProvider
  analyzeJD()
  generateInterviewScript()
  generateProjectEmphasis()
```

后续可切换 OpenAI、DeepSeek、通义千问、智谱等。

## 8. 部署架构

第一版部署：

```text
一台云服务器
  Nginx
  Docker Compose
    web
    server
    mysql
    redis
```

第二版增强：

```text
GitHub Push
  -> Jenkins Webhook
  -> Build Frontend
  -> Build Backend
  -> Docker Image
  -> Deploy
  -> Health Check
```

## 9. 关键设计约束

- LLM 不允许编造经历，输出必须包含支撑来源和风险提示。
- 公开页面不直接暴露手机号等敏感信息。
- 后台接口必须鉴权。
- Demo 接口不得影响真实内容数据。
- 第一阶段以可上线闭环为主，不追求全部功能一次完成。
