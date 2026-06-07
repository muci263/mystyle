# 智能个人作品集系统 - 接口设计文档

## 1. 接口规范

基础路径：

```text
/api
```

统一响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误响应：

```json
{
  "code": 40001,
  "message": "参数错误",
  "data": null
}
```

分页响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

## 2. 认证接口

### 2.1 登录

```http
POST /api/auth/login
```

请求：

```json
{
  "username": "admin",
  "password": "******"
}
```

响应：

```json
{
  "token": "jwt-token",
  "expiresIn": 7200,
  "user": {
    "id": 1,
    "username": "admin",
    "displayName": "赵豪然",
    "role": "ADMIN"
  }
}
```

### 2.2 获取当前用户

```http
GET /api/auth/me
```

鉴权：需要登录。

### 2.3 登出

```http
POST /api/auth/logout
```

## 3. 公开展示接口

### 3.1 获取首页数据

```http
GET /api/public/home?variantId=1
```

响应包含：

- Profile
- Featured Projects
- Featured ModuleDemos
- Skills
- Experience Snapshot
- Variant 内容，可为空
- KnowledgeGraph：首页三维个人知识图谱，包含 `nodes` 和 `edges`

### 3.1.1 获取首页知识图谱

```http
GET /api/public/knowledge-graph
```

响应：

```json
{
  "nodes": [
    {
      "nodeKey": "section-blog",
      "label": "技术博客",
      "nodeType": "SECTION",
      "level": 1,
      "summary": "技术复盘与学习笔记",
      "content": "技术博客一级节点，下面关联具体文章和对应技术主题。",
      "tags": ["Blog", "Notes"],
      "href": "/blog",
      "sourceType": "MANUAL",
      "sourceSlug": "",
      "x": 0,
      "y": -2.55,
      "z": -0.2,
      "visible": true,
      "sortOrder": 40
    }
  ],
  "edges": [
    {
      "id": 1,
      "fromNodeKey": "section-blog",
      "toNodeKey": "blog-redis-video-progress-buffer",
      "relationType": "CONTAINS",
      "visible": true,
      "sortOrder": 401
    }
  ]
}
```

说明：

- `section-blog` 对应顶部导航 04 技术博客。
- 博客文章作为 `BLOG` 子节点，通过 `CONTAINS` 关系挂在 `section-blog` 下。
- 前端 hover/click 节点时展示 `content`，不需要额外请求文章列表接口。

### 3.2 获取在线简历

```http
GET /api/public/resume?variantId=1
```

兼容旧版履历展示接口，返回 Profile、Skills、Experience、Projects。

新版结构化履历接口：

```http
GET /api/public/resume-content
```

返回草稿/发布模型中的公开履历内容：

- `version`：当前展示版本。优先返回最新 `PUBLISHED`，没有发布版本时兜底 `DRAFT`
- `basicInfo`：个人信息
- `sections.SKILL`：技术能力
- `sections.AWARD`：获奖经历
- `sections.INTERNSHIP`：实习经历
- `sections.PROJECT`：项目经历
- `sections.ADVANTAGE`：个人优势

公开接口只返回 `visible = true` 的条目。

### 3.3 获取成长路线

```http
GET /api/public/timeline
```

## 4. 项目接口

### 4.1 项目列表

```http
GET /api/public/projects?tech=Redis&variantId=1
```

### 4.2 项目详情

```http
GET /api/public/projects/{slug}?variantId=1
```

响应包含：

- 项目基础信息
- 技术栈
- 职责
- 亮点
- 证据链
- 关联 Demo
- JD Variant 强调重点

### 4.3 后台项目管理

```http
GET    /api/admin/projects
POST   /api/admin/projects
GET    /api/admin/projects/{id}
PUT    /api/admin/projects/{id}
DELETE /api/admin/projects/{id}
```

鉴权：需要管理员。

## 5. 经历资产接口

后台管理接口：

```http
GET    /api/admin/profile
PUT    /api/admin/profile

GET    /api/admin/education
POST   /api/admin/education
PUT    /api/admin/education/{id}
DELETE /api/admin/education/{id}

GET    /api/admin/experiences
POST   /api/admin/experiences
PUT    /api/admin/experiences/{id}
DELETE /api/admin/experiences/{id}

GET    /api/admin/skills
POST   /api/admin/skills
PUT    /api/admin/skills/{id}
DELETE /api/admin/skills/{id}
```

## 5.1 履历内容管理接口（第一阶段实现）

履历管理采用草稿/发布机制。第一阶段暂不接入复杂登录鉴权，但接口统一放在 `/api/admin` 下，后续可直接挂 JWT 过滤器。

### 5.1.1 获取履历草稿

```http
GET /api/admin/resume/draft
```

响应包含：

- `version`
- `basicInfo`
- `sections.SKILL`
- `sections.AWARD`
- `sections.INTERNSHIP`
- `sections.PROJECT`
- `sections.ADVANTAGE`

### 5.1.2 更新个人信息

```http
PUT /api/admin/resume/basic-info
```

请求：

```json
{
  "name": "赵豪然",
  "title": "Java 后端开发",
  "summary": "个人介绍",
  "email": "example@mail.com",
  "phone": "可为空",
  "location": "山西",
  "education": "山西大学软件工程本科",
  "githubUrl": "https://github.com/...",
  "websiteUrl": "https://..."
}
```

### 5.1.3 板块条目 CRUD

```http
GET    /api/admin/resume/sections/{sectionType}/items
POST   /api/admin/resume/sections/{sectionType}/items
PUT    /api/admin/resume/items/{itemId}
DELETE /api/admin/resume/items/{itemId}
```

`sectionType` 枚举：

- `SKILL`：技术能力
- `AWARD`：获奖经历
- `INTERNSHIP`：实习经历
- `PROJECT`：项目经历
- `ADVANTAGE`：个人优势

请求：

```json
{
  "title": "Spring Boot",
  "subtitle": "后端框架",
  "period": "",
  "summary": "核心标题或短摘要",
  "detail": "可换行保存 bullet",
  "tags": ["Java", "Backend"],
  "visible": true,
  "sortOrder": 1
}
```

### 5.1.4 发布与版本查询

```http
POST /api/admin/resume/publish
GET  /api/admin/resume/versions
```

发布会将当前草稿复制为新的 `PUBLISHED` 版本，并将旧发布版本归档。

### 5.1.5 简历上传解析与确认写入

第一阶段先支持文本内容上传，后续可替换为 PDF/DOCX 二进制解析和 LLM 结构化。

```http
POST /api/admin/resume/uploads/parse
POST /api/admin/resume/uploads/{taskId}/confirm
```

解析请求：

```json
{
  "filename": "resume.txt",
  "contentType": "text/plain",
  "content": "简历原文..."
}
```

兜底规则：

- 内容为空：返回 `FAILED`，不写入草稿。
- 能识别标题关键词：返回 `PARSED`，生成结构化预览。
- 无法识别板块：返回 `FALLBACK_REQUIRED`，保留原始文本和默认个人优势条目，等待人工确认。
- 只有调用 confirm 后，解析结果才写入草稿版本。

## 5.2 首页知识图谱管理接口（第一阶段实现）

第一阶段暂不接入复杂登录鉴权，但接口统一放在 `/api/admin` 下，后续可直接挂 JWT 过滤器。

### 5.2.1 节点 CRUD

```http
GET    /api/admin/knowledge-graph/nodes?includeHidden=true
POST   /api/admin/knowledge-graph/nodes
PUT    /api/admin/knowledge-graph/nodes/{nodeKey}
DELETE /api/admin/knowledge-graph/nodes/{nodeKey}
```

节点请求：

```json
{
  "nodeKey": "blog-test-note",
  "label": "测试博客节点",
  "nodeType": "BLOG",
  "level": 2,
  "summary": "用于测试的图谱博客节点",
  "content": "悬停节点时展示这段具体内容。",
  "tags": ["Test", "Blog"],
  "href": "/blog/test-note",
  "sourceType": "BLOG",
  "sourceSlug": "test-note",
  "x": 1.2,
  "y": -3.6,
  "z": 0.2,
  "visible": true,
  "sortOrder": 999
}
```

### 5.2.2 关系 CRUD

```http
GET    /api/admin/knowledge-graph/edges?includeHidden=true
POST   /api/admin/knowledge-graph/edges
PUT    /api/admin/knowledge-graph/edges/{edgeId}
DELETE /api/admin/knowledge-graph/edges/{edgeId}
```

关系请求：

```json
{
  "fromNodeKey": "section-blog",
  "toNodeKey": "blog-test-note",
  "relationType": "CONTAINS",
  "visible": true,
  "sortOrder": 1000
}
```

删除节点时会同步删除相关边；更新节点 `nodeKey` 时会同步更新相关边，避免图谱断链。

## 6. 工作模块复现实验室接口

### 6.1 模块列表

```http
GET /api/public/module-demos?tech=Redis&variantId=1
```

### 6.2 模块详情

```http
GET /api/public/module-demos/{slug}
```

### 6.3 后台模块管理

```http
GET    /api/admin/module-demos
POST   /api/admin/module-demos
PUT    /api/admin/module-demos/{id}
DELETE /api/admin/module-demos/{id}
```

### 6.4 视频学习 Demo

重置演示数据：

```http
POST /api/lab/video-learning/reset
```

进度上报：

```http
POST /api/lab/video-learning/progress
```

请求：

```json
{
  "userId": "demo-user",
  "courseId": "course-001",
  "sectionId": "section-001",
  "currentTime": 120,
  "duration": 600,
  "playbackRate": 1.0,
  "eventType": "timeupdate"
}
```

获取快照：

```http
GET /api/lab/video-learning/snapshot
```

响应：

```json
{
  "redisRecord": {},
  "mysqlRecord": {},
  "learningStatus": "IN_PROGRESS",
  "logs": []
}
```

完播同步：

```http
POST /api/lab/video-learning/complete
```

### 6.5 Agent 工作流 Demo

```http
GET  /api/lab/agent-workflow/templates
POST /api/lab/agent-workflow/run
GET  /api/lab/agent-workflow/runs/{runId}
```

## 7. JD 智能适配接口

### 7.1 分析 JD

```http
POST /api/jd/analyze
```

请求：

```json
{
  "jd": "岗位描述文本",
  "target": ["summary", "project_emphasis", "interview_intro"],
  "variantName": "某公司 Java 后端岗位"
}
```

响应：

```json
{
  "analysisId": 1,
  "role": "Java 后端开发实习生",
  "keywords": ["Java", "Spring Boot", "Redis", "MySQL"],
  "matchScore": 86,
  "summary": "...",
  "introShort": "...",
  "introLong": "...",
  "projectOrder": ["mine-education-system", "private-ai-platform"],
  "moduleOrder": ["video-learning", "cache-sync"],
  "projectEmphasis": [],
  "riskNotes": []
}
```

### 7.2 保存为 Variant

```http
POST /api/jd/analyses/{analysisId}/variant
```

### 7.3 获取 JD 分析历史

```http
GET /api/admin/jd/analyses
```

## 8. Profile Variant 接口

```http
GET    /api/admin/variants
POST   /api/admin/variants
GET    /api/admin/variants/{id}
PUT    /api/admin/variants/{id}
DELETE /api/admin/variants/{id}
```

公开获取：

```http
GET /api/public/variants/{id}
```

## 9. 分享链接接口

后台：

```http
GET    /api/admin/share-links
POST   /api/admin/share-links
PUT    /api/admin/share-links/{id}
DELETE /api/admin/share-links/{id}
```

公开访问：

```http
GET /api/public/share/{slug}
```

响应：

```json
{
  "variantId": 1,
  "enabled": true,
  "expired": false
}
```

## 10. 面试模式接口

```http
GET /api/public/interview?variantId=1
```

响应包含：

- 自我介绍
- 项目讲解顺序
- 推荐 Demo
- 追问问题
- 回答要点

## 11. 访问统计接口

### 11.1 记录事件

```http
POST /api/analytics/events
```

请求：

```json
{
  "eventType": "PROJECT_CLICK",
  "pagePath": "/projects/mine-education-system",
  "projectId": 1,
  "moduleDemoId": null,
  "shareSlug": "java-backend"
}
```

### 11.2 后台统计

```http
GET /api/admin/analytics/overview
GET /api/admin/analytics/projects
GET /api/admin/analytics/module-demos
GET /api/admin/analytics/share-links
```

## 12. 系统接口

健康检查：

```http
GET /api/health
```

Swagger：

```text
/swagger-ui/index.html
```
