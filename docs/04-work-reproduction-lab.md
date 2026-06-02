# 工作模块复现实验室

## 1. 设计目标

用户希望在作品集中复现自己曾经参与过的重要工作，例如视频播放学习链路、Agent 搭建、AI 平台接入等，让面试官能够直接看到过去工作中的关键模块。

这个模块应成为作品集的核心差异化能力：

> 将简历里的项目经历转化为可交互、可解释、可演示的工程模块。

面试时不只是说“我做过 Redis 缓存优化”或“我参与过 Agent 搭建”，而是打开网页现场演示：

- 用户怎么操作。
- 前端发了什么请求。
- 后端怎么处理。
- Redis / MySQL / LLM / Agent 节点发生了什么。
- 为什么这样设计。
- 出错时系统如何处理。

## 2. 模块实验室结构

建议路由：

```text
/lab
  模块实验室首页

/lab/video-learning
  视频播放与学习进度链路

/lab/cache-sync
  Redis 学习记录缓存同步

/lab/learning-state-machine
  学习状态机

/lab/agent-workflow
  Agent 搭建与流程编排

/lab/sql-bot
  SQL 智能问答

/lab/rag
  RAG 知识库检索流程

/lab/alerting
  邮件告警

/lab/file-visualization
  特殊格式文件解析与可视化
```

模块实验室必须采用可扩展设计，避免每新增一个工作经历就重写页面结构。整体采用“模块注册 + 统一外壳 + 独立 Demo 组件 + 结构化元数据”的方式实现。

## 3. 模块页统一设计

每个复现模块都采用统一结构，方便扩展：

### 3.1 顶部说明

- 模块名称
- 来源项目
- 业务背景
- 我负责的部分
- 对应技术点
- 面试讲解价值

### 3.2 交互演示区

用户可以实际操作模块，而不是只看截图。

例如：

- 播放视频。
- 触发进度上报。
- 输入自然语言问题。
- 配置 Agent 节点。
- 上传文件。
- 触发告警。

### 3.3 链路可视化区

展示一次操作背后的链路：

- Frontend Event
- API Request
- Backend Service
- Cache / Database / Queue
- External Service / LLM
- Response
- UI Update

### 3.4 技术解释区

用面试官能快速理解的方式说明：

- 问题是什么。
- 为什么这样设计。
- 方案如何落地。
- 有哪些边界条件。
- 如果要继续优化，可以怎么做。

### 3.5 日志与状态区

展示真实工程感：

- 请求日志
- 状态变化
- 节点耗时
- 错误提示
- 重试记录
- 关键数据快照

## 3.6 可扩展模块注册机制

每个工作复现模块都由两部分组成：

- 模块元数据：描述这个模块是什么、来自哪个项目、展示什么技术点、如何被 JD 匹配。
- 模块实现：具体交互 Demo，例如视频播放器、Agent 流程图、RAG 检索面板。

新增一个模块时，理想流程是：

1. 在后台或配置文件中新增 ModuleDemo 元数据。
2. 选择一个 Demo 类型，例如 `video-learning`、`agent-workflow`、`rag-flow`、`api-flow`、`custom`。
3. 绑定相关 Project、Experience、Skill。
4. 配置展示内容、接口地址、讲解点和样例数据。
5. 如果已有 Demo 类型能复用，无需新增前端组件。
6. 如果是全新交互形式，只新增一个 Demo Renderer，然后注册到模块渲染表。

前端渲染模式：

```text
ModuleDemoPage
  ├─ ModuleHeader
  ├─ BusinessContext
  ├─ DemoRenderer 根据 demoType 渲染不同组件
  ├─ FlowVisualizer
  ├─ RuntimeLogs
  ├─ TechnicalNotes
  └─ InterviewTalkingPoints
```

Demo 类型建议：

- `video-learning`：视频播放、进度上报、状态同步。
- `cache-sync`：缓存写入、延迟同步、数据库快照。
- `state-machine`：状态流转、事件驱动、事务边界。
- `agent-workflow`：Agent 节点编排、工具调用、执行日志。
- `rag-flow`：文档切块、向量检索、召回、回答生成。
- `sql-bot`：自然语言转 SQL、查询、结果解释。
- `api-flow`：普通业务接口链路复现。
- `file-parser`：文件解析、结构化结果、可视化。
- `alerting`：事件触发、告警模板、通知状态。
- `custom`：少量特殊模块使用定制组件。

## 3.7 ModuleDemo 数据模型

```json
{
  "name": "视频播放学习链路复现",
  "slug": "video-learning",
  "demoType": "video-learning",
  "sourceProject": "mine-education-system",
  "sourceExperience": "hengshan-tech-internship",
  "businessContext": "企业培训系统中记录用户视频学习进度，并驱动课程状态更新。",
  "myContribution": [
    "参与学习服务接口开发",
    "设计 Redis + 延迟任务的学习记录缓存方案",
    "处理视频与考试小节的状态流转"
  ],
  "techPoints": ["Spring Boot", "Redis", "MySQL", "状态流转", "接口设计"],
  "jdKeywords": ["Redis", "缓存优化", "性能优化", "Java 后端", "Spring Boot"],
  "difficulty": "high",
  "interviewValue": "适合展示高频写入优化、状态建模和真实业务链路设计。",
  "apiBase": "/api/lab/video-learning",
  "sampleData": {},
  "talkingPoints": [
    "为什么高频进度上报不能直接写 MySQL",
    "Redis 缓存如何降低重复写入",
    "完播和延迟任务分别在什么时机同步数据库"
  ],
  "visibility": "public",
  "sortOrder": 1
}
```

## 3.8 后台管理支持

后台需要支持模块的增删改查：

- 新增模块。
- 选择 Demo 类型。
- 绑定项目经历和实习经历。
- 编辑业务背景。
- 编辑技术点。
- 编辑面试讲解点。
- 配置样例数据。
- 配置是否公开展示。
- 配置排序和推荐权重。

后台不一定要支持在线编写复杂代码，但要能维护模块元数据。复杂交互组件仍然由代码实现，避免后台变得过重。

## 3.9 与 JD 智能适配器联动

JD 智能适配器在分析岗位后，可以根据 ModuleDemo 的 `jdKeywords`、`techPoints`、`sourceProject` 推荐最适合展示的模块。

示例：

- JD 提到 Redis、性能优化、高并发：优先展示 `video-learning`、`cache-sync`。
- JD 提到 AI 应用、Agent、RAG：优先展示 `agent-workflow`、`rag-flow`、`sql-bot`。
- JD 提到 Spring Boot、接口设计、业务系统：优先展示 `state-machine`、`api-flow`。
- JD 提到部署、Linux、Docker：优先展示部署架构页和 AI 私有化平台案例。

这样后续新增经历时，只要补充模块元数据和关键词，JD 适配器就能自动把新模块纳入推荐范围。

## 4. 视频播放学习链路复现

对应项目：矿山教育系统。

### 4.1 业务背景

企业培训系统中，用户观看课程视频时，需要记录学习进度，并在达到完成条件后更新小节、课程、课表状态。视频播放过程中进度上报频繁，如果每次都直接写数据库，会造成大量重复写入和查询压力。

### 4.2 页面交互

左侧：视频播放器模拟器。

- 播放 / 暂停
- 拖拽进度
- 倍速播放
- 模拟网络失败
- 模拟重复上报
- 手动触发完播

右侧：链路面板。

- 当前播放进度
- 最近一次上报请求
- Redis 中的学习记录
- MySQL 中的最终学习记录
- 课表学习状态
- 接口耗时

底部：事件日志。

- `play`
- `pause`
- `timeupdate`
- `progress_report`
- `cache_write`
- `db_sync`
- `state_changed`
- `error_retry`

### 4.3 后端复现接口

```http
POST /api/lab/video-learning/progress
```

用途：模拟视频进度上报。

```http
POST /api/lab/video-learning/complete
```

用途：模拟完播同步。

```http
GET /api/lab/video-learning/snapshot
```

用途：获取 Redis、MySQL、状态机快照。

```http
POST /api/lab/video-learning/reset
```

用途：重置演示数据。

### 4.4 面试讲解点

- 高频上报为什么不能每次写 MySQL。
- 前端为什么需要节流上报。
- Redis 中缓存什么数据。
- 什么时候同步数据库。
- 如何避免重复上报导致状态错乱。
- 视频小节和考试小节状态如何统一建模。

## 5. Agent 搭建与流程编排复现

对应项目：MaxKB + SqlBot 私有化 AI 管理平台。

### 5.1 业务背景

企业内部 AI 平台不只是调用一个大模型接口，还需要接入知识库、数据库、文件、图像处理等工具，并根据任务类型进行流程编排。Agent 搭建的价值在于把“用户输入 -> 工具调用 -> 模型生成 -> 结果校验”组织成可维护、可观测的流程。

### 5.2 页面交互

左侧：任务输入。

- SQL 问答任务
- 知识库问答任务
- 图像处理任务
- 自定义问题输入

中间：Agent Workflow 可视化。

节点示例：

- User Input
- Intent Classifier
- Tool Router
- SQL Generator
- SQL Safety Check
- Database Query
- RAG Retriever
- LLM Answer
- Result Validator

右侧：节点详情。

- 输入
- 输出
- 状态
- 耗时
- 错误信息
- 重试次数

底部：执行日志。

### 5.3 后端复现接口

```http
POST /api/lab/agent-workflow/run
```

用途：提交任务并执行一次 Agent 流程。

```http
GET /api/lab/agent-workflow/runs/{runId}
```

用途：获取流程执行结果。

```http
GET /api/lab/agent-workflow/templates
```

用途：获取预设 Agent 流程模板。

### 5.4 第一版实现策略

第一版可以不接真实复杂 Agent 框架，先做“工程化复现”：

- 用后端服务模拟节点执行。
- 每个节点返回结构化输入输出。
- 部分节点可接真实 LLM。
- SQL 查询使用演示数据库。
- RAG 使用小型示例知识库。
- 前端把执行过程可视化。

后续可增强：

- 接入 LangChain4j / Spring AI。
- 支持真实工具调用。
- 支持节点拖拽配置。
- 支持流程保存。

### 5.5 面试讲解点

- Agent 和普通聊天接口的区别。
- 为什么需要工具路由和结果校验。
- 如何设计节点输入输出。
- 如何记录每个节点的执行日志。
- 私有化 AI 平台为什么重视数据隔离。
- RAG、SQL Bot 和多模型任务如何统一编排。

## 6. 模块优先级建议

第一期必须做：

1. 视频播放学习链路复现。
2. Redis 学习记录缓存同步。
3. Agent 工作流复现。

第二期做：

4. SQL Bot 智能问答。
5. RAG 知识库检索流程。
6. 学习状态机。

第三期做：

7. 邮件告警。
8. 特殊格式文件解析与可视化。
9. 慢 SQL 优化前后对比。

## 7. 与可扩展内容系统的关系

每个复现模块都要能关联到：

- Project
- Experience
- Skill
- InterviewTalkingPoint
- JD Adapter 生成内容
- ModuleDemo 元数据
- DemoRenderer 类型

例如：

视频播放学习链路：

- 关联项目：矿山教育系统。
- 关联技能：Redis、Spring Boot、MySQL、状态流转、接口设计。
- 关联经历：学习服务接口开发、视频进度缓存方案。
- JD 适配时：如果岗位要求 Redis、性能优化、业务状态流转，则优先展示该模块。

Agent 工作流：

- 关联项目：MaxKB + SqlBot 私有化 AI 管理平台。
- 关联技能：Spring AI、LangGraph、RAG、SQL Bot、Docker、Nginx。
- 关联经历：AI 平台接入、SQL 问答样例验证、RAG 流程优化。
- JD 适配时：如果岗位要求 AI 应用、Agent、RAG、LLM 工程化，则优先展示该模块。

## 8. 产品价值

这部分会让作品集从普通展示页变成真正的“项目能力证明系统”：

- 面试官能看到实际交互。
- 你能围绕真实模块讲工程细节。
- 每个 Demo 都能反向支撑简历。
- JD 智能适配器还能根据岗位要求自动推荐最该展示的模块。

推荐对外表达：

> 我把自己在实习和项目中参与过的关键模块抽象成了可交互 Demo，例如视频学习进度上报、Redis 缓存同步、Agent 工作流和 RAG 检索流程。这样面试时不只是口头描述项目，而是可以直接展示一次完整的业务链路和背后的工程设计。
