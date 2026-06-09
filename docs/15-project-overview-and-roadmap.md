# 智能个人作品集系统 - 项目总览与上手规划

## 1. 文档目的

本文用于交接和继续开发整个项目，重点回答四个问题：

- 这个项目最终要做成什么。
- 当前代码已经实现了什么。
- 后续还需要对接和增强什么。
- 新开发者如何快速启动、理解、修改和扩展项目。

项目定位不是普通静态个人主页，而是一个可真实运行、可后台维护、可用于秋招面试展示的工程化作品集系统。它需要同时体现前端表现力、后端业务建模能力、数据库设计能力、AI 接入能力和部署工程能力。

## 2. 项目目标

### 2.1 核心目标

构建一个面向 Java 后端秋招面试的高级个人展示系统，能把个人履历、项目证据、工作模块复现、技术博客、JD 适配和系统架构以统一产品形态展示出来。

### 2.2 面试展示目标

- 第一眼看到个人定位、技术方向和工程能力。
- 面试官可以直接查看在线简历，而不是只看 PDF。
- 项目经历不是文字堆叠，而是能看到证据链、核心难点、指标和可运行 Demo。
- 曾经参与过的功能模块可以在实验室中复现，例如视频学习链路、Redis 缓存同步、Agent 工作流等。
- 输入岗位 JD 后，可以由 LLM 帮助重排项目重点、生成岗位适配说明和面试讲解思路。
- 技术博客可以沉淀学习总结、实习复盘、AI 学习记录和项目复盘。
- 整个系统通过 Docker 统一启动，后续可接 Jenkins 自动部署。

### 2.3 工程目标

- 前后端分离。
- 数据库驱动内容，不依赖纯静态数据。
- 主要内容可编辑、可扩展。
- 后端接口规范统一，具备异常处理、CORS、健康检查和测试。
- 前端具备高级交互，包括三维知识图谱、滚动动效、鼠标流光轨迹和响应式页面。
- AI 能力通过统一 LLM Provider 扩展，当前优先预留 Minimax。

## 3. 技术架构

### 3.1 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Next.js 15, React 19, TypeScript, Tailwind CSS, Framer Motion |
| 三维展示 | Three.js, React Three Fiber, Drei |
| 后端 | Spring Boot 3, Java 21, Maven |
| 数据库 | MySQL 8.4 |
| 缓存/状态模拟 | Redis 7.4 |
| 部署 | Docker Compose, Nginx |
| AI Provider | Minimax 预留，未配置 Key 时使用规则兜底 |
| 测试 | Spring Boot Test, Next.js typecheck/build |

### 3.2 运行拓扑

```text
Browser
  |
  v
Nginx :3000
  |-- /               -> Next.js web :3000
  |-- /api/*          -> Spring Boot server :8080
                         |
                         |-- MySQL :3306
                         |-- Redis :6379
```

### 3.3 目录结构

```text
apps
  web
    app                 Next.js App Router 页面
    components          全局组件、三维图谱、滚动首页、导航、光标特效
    lib                 API 类型和请求封装
  server
    src/main/java       Spring Boot 业务代码
    src/main/resources  application.yml 和 data.sql
docs                    需求、接口、数据库、设计与本文档
infra
  docker                Docker Compose、本地 MySQL 初始化
  nginx                 Nginx 反向代理配置
  jenkins               Jenkins 相关配置预留
```

## 4. 当前已实现功能

### 4.1 前端页面

| 页面 | 路径 | 当前状态 |
| --- | --- | --- |
| 首页 | `/` | 已实现三维个人知识图谱、个人基础信息、节点详情浮现、滚动进入后续区块 |
| 在线简历 | `/resume` | 已接入后端公开履历内容 |
| 项目证据 | `/evidence`, `/projects`, `/projects/[slug]` | 已实现项目列表、项目详情和证据展示入口 |
| 面试助手 | `/interview`, `/interview-kit` | 已实现面试讲解页面和岗位适配展示入口 |
| 技术博客 | `/blog`, `/blog/[slug]`, `/blog/new`, `/blog/[slug]/edit` | 已实现博客列表、详情、新增、编辑、评论、点赞、旁注 |
| 经典功能复现 | `/lab`, `/lab/[slug]` | 已实现实验室首页和模块详情入口 |
| JD 适配 | `/jd-adapter` | 已实现基础页面和后端接口对接 |
| 图谱管理 | `/admin/knowledge-graph` | 已实现节点/边 CRUD、类别分组、智能建边入口 |
| 简历管理 | `/admin/resume` | 已实现履历草稿、分区维护、上传解析任务入口 |

### 4.2 首页三维知识图谱

已实现能力：

- 以 `knowledge_graph_node` 和 `knowledge_graph_edge` 数据表驱动首页图谱。
- 节点类型支持 `CORE`、`SECTION`、`SKILL`、`PROJECT`、`MODULE`、`BLOG`。
- 首页中心使用三维知识图谱展示个人能力结构。
- 左侧展示个人核心信息，右侧展示当前 hover/click 节点详情。
- 右侧节点详情在用户移出后延迟隐藏。
- 图谱支持鼠标滚轮缩放。
- 缩放到最小视觉尺寸后，继续向下滚轮会释放给页面滚动。
- 前端使用确定性 force layout 优化节点分布，降低重叠概率。
- 标签在密集场景下弱化，hover 或 active 时突出展示。

相关文件：

- `apps/web/components/knowledge-graph-scene.tsx`
- `apps/web/components/scroll-portfolio.tsx`
- `apps/web/app/admin/knowledge-graph`
- `apps/server/src/main/java/com/mystyle/portfolio/knowledge`

### 4.3 图谱后台管理

已实现能力：

- 节点列表按类别分组展示。
- 支持按类别筛选节点。
- 支持新增节点、编辑节点、删除节点。
- 支持新增关系、删除关系。
- 新增节点时只需要填写标题、类别、简介、标签、可见性等关键信息。
- `nodeKey`、跳转路径、来源、坐标、排序等字段放入高级设置。
- 支持对已有节点触发 AI/规则建边。
- 支持智能创建节点并自动保存推荐关系。

后端接口：

```http
GET    /api/public/knowledge-graph
GET    /api/admin/knowledge-graph/nodes
POST   /api/admin/knowledge-graph/nodes
POST   /api/admin/knowledge-graph/nodes/smart-create
POST   /api/admin/knowledge-graph/nodes/{nodeKey}/auto-relate
PUT    /api/admin/knowledge-graph/nodes/{nodeKey}
DELETE /api/admin/knowledge-graph/nodes/{nodeKey}
GET    /api/admin/knowledge-graph/edges
POST   /api/admin/knowledge-graph/edges
PUT    /api/admin/knowledge-graph/edges/{edgeId}
DELETE /api/admin/knowledge-graph/edges/{edgeId}
```

### 4.4 简历内容管理

已实现能力：

- 将个人简历拆成可编辑结构。
- 支持个人基础信息维护。
- 支持以下板块条目维护：
  - 技术能力
  - 获奖经历
  - 实习经历
  - 项目经历
  - 个人优势
- 支持草稿/发布版本思路。
- 支持简历上传解析任务的接口入口。
- 上传解析失败时有任务状态和错误信息兜底，不直接覆盖真实数据。

后端接口：

```http
GET    /api/public/resume-content
GET    /api/admin/resume/draft
PUT    /api/admin/resume/basic-info
GET    /api/admin/resume/sections/{sectionType}/items
POST   /api/admin/resume/sections/{sectionType}/items
PUT    /api/admin/resume/items/{itemId}
DELETE /api/admin/resume/items/{itemId}
POST   /api/admin/resume/publish
GET    /api/admin/resume/versions
POST   /api/admin/resume/uploads/parse
GET    /api/admin/resume/uploads/{taskId}
POST   /api/admin/resume/uploads/{taskId}/confirm
```

### 4.5 技术博客

已实现能力：

- 博客列表。
- 博客分类。
- 新增博客。
- 编辑博客。
- 博客详情。
- 点赞。
- 评论。
- 旁注。
- 旁注支持对选中文本添加说明。
- 旁注高亮显示，hover 后展示旁注内容。
- 旁注支持修改和删除。
- 博客文章可作为知识图谱 `BLOG` 节点挂到 `技术博客` 板块下。

博客类型建议：

- 实习心得
- AI 学习
- Java 后端
- 项目复盘
- 面试总结
- 工程化

后端接口：

```http
GET    /api/public/blog-posts
POST   /api/public/blog-posts
PUT    /api/public/blog-posts/{slug}
GET    /api/public/blog-posts/categories
GET    /api/public/blog-posts/{slug}
GET    /api/public/blog-posts/{slug}/comments
POST   /api/public/blog-posts/{slug}/comments
GET    /api/public/blog-posts/{slug}/annotations
POST   /api/public/blog-posts/{slug}/annotations
PUT    /api/public/blog-posts/{slug}/annotations/{annotationId}
DELETE /api/public/blog-posts/{slug}/annotations/{annotationId}
POST   /api/public/blog-posts/{slug}/likes
GET    /api/public/blog-posts/{slug}/interactions
```

### 4.6 项目证据与模块复现

已实现能力：

- 项目列表。
- 项目详情。
- 首页展示项目证据入口。
- 实验室模块列表。
- 实验室模块详情。
- 视频学习链路 Demo。
- Agent 工作流 Demo。
- Redis/MySQL/状态流转等工程概念展示。

后端接口：

```http
GET /api/public/projects
GET /api/public/projects/{slug}

GET /api/public/module-demos
GET /api/public/module-demos/{slug}

POST /api/lab/video-learning/reset
POST /api/lab/video-learning/progress
POST /api/lab/video-learning/complete
GET  /api/lab/video-learning/snapshot

GET  /api/lab/agent-workflow/templates
POST /api/lab/agent-workflow/run
GET  /api/lab/agent-workflow/runs/{runId}
```

### 4.7 JD 适配与 LLM 能力

已实现能力：

- JD 分析接口。
- 基于岗位 JD 返回匹配分、关键词、项目排序、介绍建议、风险提醒。
- LLM Provider 抽象已加入。
- Minimax 配置项已加入。
- 未配置 Minimax API Key 时，使用规则型 fallback，不影响接口可用。
- 简历优化接口已预留。
- 图谱智能建边已经复用 LLM Provider。

接口：

```http
POST /api/jd/analyze
POST /api/jd/analyses/{analysisId}/variant

GET  /api/llm/status
POST /api/llm/resume/optimize
```

环境变量：

```env
MINIMAX_API_KEY=
MINIMAX_BASE_URL=https://api.minimax.io/v1
MINIMAX_TEXT_MODEL=MiniMax-M1
MINIMAX_VISION_MODEL=MiniMax-Text-01
```

当前如果没有配置 Key：

```json
{
  "provider": "mock-rule-provider",
  "configured": false,
  "mode": "fallback"
}
```

### 4.8 工程化与部署

已实现能力：

- Docker Compose 本地一键启动。
- Nginx 统一暴露 `localhost:3000`。
- Web 和 Server 不再分别暴露多个随机端口。
- MySQL 和 Redis 由 Docker 管理。
- 后端健康检查。
- CORS 配置。
- 统一 API 响应结构。
- 全局异常处理。
- 后端冒烟测试。
- 前端类型检查和生产构建。

常用命令：

```bash
npm run docker:up
npm run docker:down
npm run docker:ps
npm run docker:logs
npm --workspace apps/web run typecheck
npm run build:web
npm run test:server
```

## 5. 当前数据库能力

当前数据库已经覆盖以下核心方向：

- 个人公开展示内容。
- 项目经历。
- 模块复现。
- 在线简历草稿和发布内容。
- 简历上传解析任务。
- 技术博客。
- 博客评论、点赞、旁注。
- 首页知识图谱节点和关系。
- JD 分析结果。
- 访问统计事件。

关键表包括：

```text
resume_version
resume_basic_info
resume_section_item
resume_upload_task
knowledge_graph_node
knowledge_graph_edge
blog_post
blog_comment
blog_annotation
project
module_demo
jd_analysis
profile_variant
visit_event
```

详细字段见：

- `docs/09-database-design.md`
- `apps/server/src/main/resources/data.sql`
- `infra/docker/mysql/init.sql`

## 6. 后续待对接和待实现功能

### 6.1 认证与权限

当前后台接口已经使用 `/api/admin` 路径区分，但还没有完整登录鉴权。后续需要实现：

- 管理员登录。
- JWT 签发与刷新。
- 后台接口鉴权过滤器。
- 公开接口和后台接口权限隔离。
- 操作日志。

优先级：高。  
原因：项目真实上线前必须补齐，否则后台管理接口会暴露。

### 6.2 Minimax 真实接入

当前 LLM Provider 已经具备 Minimax 配置位和 fallback。后续需要：

- 配置真实 `MINIMAX_API_KEY`。
- 验证文本模型调用。
- 验证视觉/多模态模型调用。
- 优化 Prompt。
- 为不同任务拆分 Prompt 模板。
- 记录 LLM 调用日志和错误信息。
- 控制超时、重试、最大 token 和费用风险。

建议拆成三个能力：

1. 图谱自动建边。
2. JD 适配增强。
3. 简历优化和上传解析增强。

### 6.3 简历上传解析增强

当前上传解析接口已预留，但真实解析能力还需要增强：

- 支持 PDF 文本抽取。
- 支持 Word 文档抽取。
- 支持图片简历 OCR 或多模态理解。
- LLM 将原始简历拆成结构化板块。
- 解析结果先进入预览，不直接覆盖已有内容。
- 用户确认后写入草稿。
- 增加冲突合并策略，例如同名项目更新而不是重复创建。

### 6.4 后台管理完善

已实现简历管理和知识图谱管理，后续建议补齐：

- 项目管理。
- 模块复现管理。
- 博客分类管理。
- JD 分析历史管理。
- Profile Variant 管理。
- 分享链接管理。
- 首页展示配置管理。
- 图谱节点批量导入/导出。

### 6.5 经典功能复现实验室增强

当前实验室有基础模块，后续可以做成面试中的亮点：

- 视频学习链路：
  - 进度上报防抖。
  - Redis 临时状态。
  - 完播后 MySQL 落库。
  - 异常恢复和幂等处理。
- Agent 工作流：
  - 工具选择。
  - RAG 检索。
  - SQL Bot。
  - 执行轨迹可视化。
- Cache Sync：
  - 延迟双删。
  - 缓存击穿/穿透/雪崩模拟。
  - Redisson 锁演示。
- RAG Flow：
  - 文档切片。
  - 向量检索。
  - 重排。
  - 引用溯源。

### 6.6 前端体验继续优化

当前首页已进入三维知识图谱方向，后续建议：

- 图谱节点分布继续调参。
- 节点拖拽后保存坐标。
- 图谱搜索。
- 图谱类别开关。
- 图谱路径高亮，例如点击 Redis 后高亮相关项目、博客、Demo。
- 移动端专门降级为 2D 关系图或卡片节点流。
- 子页面统一做成同一设计语言，减少黑白割裂。
- 页面切换增加更顺滑的过渡。

### 6.7 CI/CD 与上线

当前 Docker 本地闭环已经可用，后续上线需要：

- 服务器环境准备。
- Nginx 域名和 HTTPS。
- Jenkins Pipeline 完整接入。
- GitHub Webhook。
- 自动构建前端镜像。
- 自动构建后端镜像。
- 自动部署 Docker Compose。
- 数据库备份。
- 健康检查失败回滚。
- 日志采集。

## 7. 快速上手

### 7.1 本地 Docker 启动

推荐使用 Docker 启动完整链路：

```bash
cp .env.example .env
npm run docker:up
```

访问：

```text
http://localhost:3000
```

查看容器：

```bash
npm run docker:ps
```

查看日志：

```bash
npm run docker:logs
```

停止：

```bash
npm run docker:down
```

### 7.2 本地开发模式

前端：

```bash
npm run dev:web
```

后端：

```bash
npm run dev:server
```

注意：本地开发模式需要确保 MySQL 和 Redis 可用。更推荐先用 Docker 跑数据库，再单独启动前后端做调试。

### 7.3 常用验证

前端类型检查：

```bash
npm --workspace apps/web run typecheck
```

前端生产构建：

```bash
npm run build:web
```

后端测试：

```bash
npm run test:server
```

后端健康检查：

```bash
curl http://localhost:3000/api/health
```

LLM 状态：

```bash
curl http://localhost:3000/api/llm/status
```

知识图谱公开数据：

```bash
curl http://localhost:3000/api/public/knowledge-graph
```

## 8. 继续开发时应该先看哪些文件

### 8.1 首页和知识图谱

```text
apps/web/app/page.tsx
apps/web/components/scroll-portfolio.tsx
apps/web/components/knowledge-graph-scene.tsx
apps/web/app/admin/knowledge-graph
apps/server/src/main/java/com/mystyle/portfolio/knowledge
```

### 8.2 简历管理

```text
apps/web/app/resume/page.tsx
apps/web/app/admin/resume/page.tsx
apps/server/src/main/java/com/mystyle/portfolio/resume
```

### 8.3 博客

```text
apps/web/app/blog
apps/server/src/main/java/com/mystyle/portfolio/blog
```

### 8.4 JD 与 LLM

```text
apps/web/app/jd-adapter/page.tsx
apps/server/src/main/java/com/mystyle/portfolio/jd
apps/server/src/main/java/com/mystyle/portfolio/llm
```

### 8.5 模块复现

```text
apps/web/app/lab
apps/server/src/main/java/com/mystyle/portfolio/moduleDemo
```

## 9. 常见扩展动作

### 9.1 新增一段简历经历

推荐走后台页面：

```text
http://localhost:3000/admin/resume
```

操作思路：

1. 进入对应板块，例如实习经历、项目经历、获奖经历。
2. 新增条目，填写标题、副标题、时间、摘要、详情、标签。
3. 确认 `visible = true`。
4. 发布履历版本。
5. 前台 `/resume` 验证展示结果。

如果需要同时在首页知识图谱出现，需要再到 `/admin/knowledge-graph` 新增对应节点，并建立关系。

### 9.2 新增一篇技术博客

推荐走前端页面：

```text
http://localhost:3000/blog/new
```

操作思路：

1. 选择博客分类。
2. 填写标题、摘要、正文和标签。
3. 保存后进入博客详情页。
4. 如需要在首页图谱中展示，到 `/admin/knowledge-graph` 新增 `BLOG` 节点。
5. 将该节点与 `section-blog` 建立 `CONTAINS` 关系。

后续可优化为：创建博客后自动生成 `BLOG` 图谱节点，并由 LLM 自动建边。

### 9.3 新增一个图谱节点

推荐走图谱管理页：

```text
http://localhost:3000/admin/knowledge-graph
```

操作思路：

1. 点击新增节点。
2. 填写标题、类别、简介、标签、可见性。
3. 高级字段没有特殊需要可以不填。
4. 保存后系统会尝试用 LLM/fallback 自动生成关系。
5. 如关系不准确，可以手动删除或新增边。
6. 刷新首页验证图谱。

节点类别建议：

| 类别 | 用途 |
| --- | --- |
| `CORE` | 个人中心节点，通常只保留一个 |
| `SECTION` | 一级板块，例如履历、项目证据、面试助手、技术博客 |
| `SKILL` | 技术能力，例如 Java、Redis、Spring Boot |
| `PROJECT` | 项目经历 |
| `MODULE` | 功能复现模块 |
| `BLOG` | 技术文章 |

### 9.4 新增一个经典功能复现模块

当前建议先按代码扩展，后续再做后台配置化：

1. 在数据库或种子数据中新增 `module_demo`。
2. 在 `apps/web/app/lab/[slug]` 的渲染逻辑中增加对应模块展示。
3. 如需要后端交互，在 `apps/server/src/main/java/com/mystyle/portfolio/moduleDemo` 下新增 Controller 和 Service。
4. 在 `apps/web/lib/api.ts` 增加前端类型。
5. 在首页或图谱中新增 `MODULE` 节点并关联到对应项目/技术。
6. 补充后端测试和前端构建验证。

### 9.5 接入真实 Minimax

操作思路：

1. 在 `.env` 中配置：

```env
MINIMAX_API_KEY=你的 Key
MINIMAX_BASE_URL=https://api.minimax.io/v1
MINIMAX_TEXT_MODEL=MiniMax-M1
MINIMAX_VISION_MODEL=MiniMax-Text-01
```

2. 重启 Docker：

```bash
npm run docker:up
```

3. 检查 Provider：

```bash
curl http://localhost:3000/api/llm/status
```

4. 验证图谱自动建边和 JD 适配输出。
5. 如果调用失败，系统应自动回退到规则 fallback，前端不应直接崩溃。

## 10. 推荐迭代顺序

建议后续按下面顺序推进，不要同时改太多方向：

1. 补齐认证与后台权限。
2. 接入真实 Minimax Key，验证 LLM Provider。
3. 完善简历上传解析和确认写入。
4. 强化 JD 适配：项目排序、个人介绍、面试问题、风险提示。
5. 强化知识图谱：搜索、类别过滤、路径高亮、拖拽保存。
6. 扩展模块复现实验室，把 Redis、Agent、RAG、SQL Bot 做成真正可操作 Demo。
7. 补齐 Jenkins 自动部署和线上 HTTPS。
8. 做最终前端视觉统一和移动端适配。

## 11. 当前风险与注意事项

- 后台接口尚未完整鉴权，不能直接暴露公网。
- Minimax API Key 不要写入代码仓库，只能走环境变量。
- Docker MySQL 使用 volume 保存数据，修改 `data.sql` 不一定会覆盖已有 volume 中的数据。
- 如果需要重建数据库，需要先备份，再删除对应 Docker volume。
- 前端首页依赖三维渲染，移动端和低性能设备需要持续做降级优化。
- 知识图谱的 force layout 是前端确定性布局，数据库坐标仍然保留为基础数据。
- 简历上传解析目前是结构预留，真实 PDF/Word/OCR 能力仍需继续做。

## 12. 面试讲解建议

可以把项目讲成四层：

1. 展示层：Next.js + Three.js，将个人履历抽象成知识图谱。
2. 内容层：简历、项目、博客、模块 Demo 都是数据库驱动，可编辑、可扩展。
3. 智能层：LLM Provider 支撑 JD 适配、图谱建边、简历优化。
4. 工程层：Spring Boot + MySQL + Redis + Docker + Nginx，后续接 Jenkins 自动部署。

推荐讲解路径：

```text
首页知识图谱
  -> 在线简历
  -> 项目证据
  -> 经典功能复现
  -> 技术博客
  -> JD 适配
  -> Docker/Nginx/后端接口/数据库设计
```

这条路径能同时展示前端观感、后端建模、数据库、AI 接入和工程部署能力。
