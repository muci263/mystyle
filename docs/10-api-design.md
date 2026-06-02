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

### 3.2 获取在线简历

```http
GET /api/public/resume?variantId=1
```

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
