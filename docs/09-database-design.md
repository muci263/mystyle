# 智能个人作品集系统 - 数据库设计文档

## 1. 设计原则

- 数据结构服务于可扩展经历资产库。
- 公开展示、后台管理、JD 适配、模块实验室共享同一套基础数据。
- 关键实体保留 `visibility`、`sort_order`、`created_at`、`updated_at` 字段。
- LLM 生成内容与原始真实经历分离，避免污染事实数据。
- 访问统计只记录匿名事件，不采集敏感个人信息。
- 履历内容采用“草稿版本 -> 发布版本”的管理方式，前台展示只读取已发布版本。
- 简历上传解析结果先进入解析任务表，必须确认后才写入草稿，避免解析错误覆盖真实数据。

## 2. 核心实体

```text
user
profile
education
experience
project
skill
module_demo
evidence_chain
article
jd_analysis
profile_variant
share_link
visit_event
operation_log
```

## 3. 表结构草案

### 3.0 resume_version

履历版本表。第一阶段用于维护草稿、发布和后续回滚能力。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| version_name | varchar(128) | 版本名称 |
| status | varchar(32) | `DRAFT` / `PUBLISHED` / `ARCHIVED` |
| source_task_id | bigint | 来源解析任务，可为空 |
| published_at | datetime | 发布时间，可为空 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.0.1 resume_basic_info

个人基础信息，按版本归档。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| version_id | bigint | 履历版本 |
| name | varchar(64) | 姓名 |
| title | varchar(128) | 求职方向/标题 |
| summary | text | 个人介绍 |
| email | varchar(128) | 邮箱 |
| phone | varchar(64) | 手机号 |
| location | varchar(128) | 所在地 |
| education | varchar(128) | 教育摘要 |
| github_url | varchar(255) | GitHub |
| website_url | varchar(255) | 个人站点 |
| updated_at | datetime | 更新时间 |

### 3.0.2 resume_section_item

统一承载技术能力、获奖经历、实习经历、项目经历、个人优势。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| version_id | bigint | 履历版本 |
| section_type | varchar(32) | `SKILL` / `AWARD` / `INTERNSHIP` / `PROJECT` / `ADVANTAGE` |
| title | varchar(160) | 标题 |
| subtitle | varchar(160) | 副标题，如公司/技能分组/项目角色 |
| period | varchar(64) | 时间范围 |
| summary | text | 简短摘要 |
| detail | text | 细节，可存放换行列表或 JSON 文本 |
| tags | text | 标签 JSON 字符串，第一阶段用文本保存 |
| visible | tinyint | 是否展示 |
| sort_order | int | 排序 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.0.3 resume_upload_task

简历上传解析任务。用于上传读取、结构化预览、失败兜底和确认写入。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| filename | varchar(255) | 文件名 |
| content_type | varchar(128) | 文件类型 |
| status | varchar(32) | `PARSED` / `FALLBACK_REQUIRED` / `CONFIRMED` / `FAILED` |
| raw_text | longtext | 原始文本 |
| parsed_json | longtext | 结构化结果 JSON |
| error_message | text | 失败原因或兜底提示 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.0.4 knowledge_graph_node

首页三维知识图谱节点表。用于把个人核心、一级板块、技术能力、项目、模块复现、技术博客文章统一建模，前端按该表渲染节点。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| node_key | varchar(128) | 节点业务标识，唯一 |
| label | varchar(160) | 节点显示标题 |
| node_type | varchar(32) | `CORE` / `SECTION` / `SKILL` / `PROJECT` / `MODULE` / `BLOG` |
| level | int | 节点层级，0 为个人核心，1 为一级板块，2 为子节点 |
| summary | text | 简短说明 |
| content | text | 悬停/点击节点时展示的具体内容 |
| tags | text | 逗号分隔标签 |
| href | varchar(255) | 关联前端路径，可为空 |
| source_type | varchar(32) | 来源类型，如 `MANUAL` / `PROJECT` / `MODULE` / `BLOG` |
| source_slug | varchar(128) | 来源业务 slug，可为空 |
| x / y / z | double | 3D 图谱坐标 |
| visible | tinyint | 是否前台展示 |
| sort_order | int | 排序 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

默认数据中 `section-blog` 表示导航 04 技术博客，博客文章节点使用 `node_type = BLOG`，并通过边表挂在 `section-blog` 下。

### 3.0.5 knowledge_graph_edge

首页三维知识图谱关系表。用于描述节点之间的父子、包含、使用、解释等关系。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| from_node_key | varchar(128) | 起点节点 key |
| to_node_key | varchar(128) | 终点节点 key |
| relation_type | varchar(64) | `OWNS` / `INCLUDES` / `CONTAINS` / `USES` / `EXPLAINS` 等 |
| visible | tinyint | 是否前台展示 |
| sort_order | int | 排序 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.1 user

管理员用户表。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| username | varchar(64) | 用户名 |
| password_hash | varchar(255) | 密码哈希 |
| display_name | varchar(64) | 显示名 |
| role | varchar(32) | 角色 |
| enabled | tinyint | 是否启用 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.2 profile

个人基础信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| name | varchar(64) | 姓名 |
| title | varchar(128) | 标题/求职方向 |
| summary | text | 个人简介 |
| avatar_url | varchar(255) | 头像地址 |
| email | varchar(128) | 邮箱 |
| github_url | varchar(255) | GitHub |
| resume_pdf_url | varchar(255) | 简历 PDF |
| current_status | varchar(128) | 当前状态 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.3 education

教育经历。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| school | varchar(128) | 学校 |
| degree | varchar(64) | 学历 |
| major | varchar(128) | 专业 |
| start_date | date | 开始时间 |
| end_date | date | 结束时间 |
| courses | json | 课程 |
| achievements | json | 奖项 |
| description | text | 描述 |
| sort_order | int | 排序 |
| visibility | varchar(32) | 可见性 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.4 experience

实习/工作经历。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| company | varchar(128) | 公司 |
| position | varchar(128) | 岗位 |
| start_date | date | 开始时间 |
| end_date | date | 结束时间，可为空 |
| summary | text | 概述 |
| responsibilities | json | 工作职责 |
| achievements | json | 量化成果 |
| tech_tags | json | 技术标签 |
| visibility | varchar(32) | 可见性 |
| sort_order | int | 排序 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.5 project

项目经历。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| name | varchar(128) | 项目名 |
| slug | varchar(128) | 路由标识 |
| role | varchar(128) | 我的角色 |
| start_date | date | 开始时间 |
| end_date | date | 结束时间 |
| summary | text | 项目摘要 |
| background | text | 项目背景 |
| tech_stack | json | 技术栈 |
| responsibilities | json | 我的职责 |
| highlights | json | 项目亮点 |
| challenges | json | 难点 |
| solutions | json | 解决方案 |
| metrics | json | 指标 |
| architecture | json | 架构说明 |
| interview_talking_points | json | 面试讲解点 |
| visibility | varchar(32) | 可见性 |
| sort_order | int | 排序 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.6 skill

技能表。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| category | varchar(64) | 分类 |
| name | varchar(64) | 技能名 |
| level | varchar(32) | 熟练度 |
| description | text | 描述 |
| sort_order | int | 排序 |
| visibility | varchar(32) | 可见性 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.7 module_demo

工作复现模块。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| name | varchar(128) | 模块名称 |
| slug | varchar(128) | 路由标识 |
| demo_type | varchar(64) | Demo Renderer 类型 |
| source_project_id | bigint | 来源项目 |
| source_experience_id | bigint | 来源经历 |
| business_context | text | 业务背景 |
| my_contribution | json | 我的贡献 |
| tech_points | json | 技术点 |
| jd_keywords | json | JD 匹配关键词 |
| difficulty | varchar(32) | 难度 |
| interview_value | text | 面试价值 |
| api_base | varchar(255) | 演示接口前缀 |
| sample_data | json | 样例数据 |
| talking_points | json | 讲解点 |
| visibility | varchar(32) | 可见性 |
| sort_order | int | 排序 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.8 evidence_chain

项目证据链。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| project_id | bigint | 所属项目 |
| module_demo_id | bigint | 关联 Demo，可为空 |
| problem | text | 业务问题 |
| solution | text | 解决方案 |
| implementation | text | 技术实现 |
| metrics | json | 结果指标 |
| proof_points | json | 证据点 |
| interview_questions | json | 面试追问 |
| sort_order | int | 排序 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.9 article

复盘文章。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| title | varchar(128) | 标题 |
| slug | varchar(128) | 路由标识 |
| summary | text | 摘要 |
| content | longtext | 正文 |
| tags | json | 标签 |
| related_project_id | bigint | 关联项目 |
| visibility | varchar(32) | 可见性 |
| published_at | datetime | 发布时间 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.10 jd_analysis

JD 分析记录。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| jd_hash | varchar(64) | JD 哈希 |
| jd_text | longtext | JD 原文 |
| target_role | varchar(128) | 岗位方向 |
| keywords | json | 关键词 |
| match_score | int | 匹配度 |
| result_json | json | LLM 结构化结果 |
| model_name | varchar(128) | 使用模型 |
| prompt_version | varchar(64) | Prompt 版本 |
| created_at | datetime | 创建时间 |

### 3.11 profile_variant

岗位适配展示版本。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| name | varchar(128) | Variant 名称 |
| jd_analysis_id | bigint | 来源 JD 分析 |
| target_role | varchar(128) | 岗位方向 |
| generated_summary | text | 生成摘要 |
| intro_short | text | 短自我介绍 |
| intro_long | text | 长自我介绍 |
| project_order | json | 项目排序 |
| module_order | json | 模块排序 |
| project_emphasis | json | 项目强调重点 |
| skill_emphasis | json | 技能强调重点 |
| risk_notes | json | 风险提示 |
| enabled | tinyint | 是否启用 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.12 share_link

定制分享链接。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| slug | varchar(128) | 分享链接标识 |
| name | varchar(128) | 名称 |
| profile_variant_id | bigint | 绑定 Variant |
| enabled | tinyint | 是否启用 |
| expires_at | datetime | 过期时间 |
| visit_count | int | 访问次数 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.13 visit_event

匿名访问事件。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| event_type | varchar(64) | 事件类型 |
| page_path | varchar(255) | 页面路径 |
| project_id | bigint | 项目 ID，可为空 |
| module_demo_id | bigint | Demo ID，可为空 |
| share_link_id | bigint | 分享链接 ID，可为空 |
| referrer | varchar(255) | 来源 |
| user_agent_hash | varchar(64) | UA 哈希 |
| ip_hash | varchar(64) | IP 哈希 |
| created_at | datetime | 创建时间 |

### 3.14 operation_log

后台操作日志。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| user_id | bigint | 操作用户 |
| action | varchar(128) | 操作 |
| target_type | varchar(64) | 对象类型 |
| target_id | bigint | 对象 ID |
| detail | json | 操作详情 |
| created_at | datetime | 创建时间 |

## 4. 关联表建议

为支持多对多关联，建议增加：

```text
project_skill
experience_project
experience_skill
module_demo_skill
project_module_demo
```

第一期可根据开发复杂度，先使用 JSON 字段保存轻量关联，后续再规范化为关联表。

## 5. 索引设计

建议索引：

- `project.slug` 唯一索引
- `module_demo.slug` 唯一索引
- `article.slug` 唯一索引
- `share_link.slug` 唯一索引
- `jd_analysis.jd_hash` 普通索引
- `visit_event.created_at` 普通索引
- `visit_event.event_type` 普通索引
- `profile_variant.enabled` 普通索引

## 6. 数据安全

- 密码只存哈希。
- LLM API Key 不入库，使用环境变量。
- 公开页面不直接展示敏感字段。
- 访问统计使用 hash，不保存原始 IP。
- 删除重要内容建议先软删除，第一期可用 `visibility = hidden` 代替。
