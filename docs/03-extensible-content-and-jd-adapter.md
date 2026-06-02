# 可扩展经历系统与 JD 智能适配器

## 1. 新增产品想法记录

用户希望：

- 项目经历和实习经历不要写死在页面里，而是做成可扩展内容。
- 后续新增实习、项目、比赛、文章时，只需要在后台或数据文件中维护内容，页面自动适配。
- 网站增加一个 JD 输入功能：用户粘贴目标岗位 JD 后，后端调用 LLM，返回岗位分析、个人介绍、项目强调重点等内容。
- 页面部分区域可以基于 JD 适配结果重新展示，让同一套个人经历面向不同岗位时有不同表达重点。

这个方向可以作为项目的核心差异化能力：

> 从“个人作品集”升级为“可动态适配岗位的智能个人展示系统”。

## 2. 内容可扩展设计

经历、项目、技能、模块 Demo 都应当数据化，而不是硬编码在组件里。

### 2.1 核心原则

- 页面组件只负责展示。
- 内容由数据库或 MDX/JSON 数据源提供。
- 每条经历都支持标签、技术栈、量化成果、关联模块。
- 新增内容后，首页、简历页、项目页、模块实验室自动更新。
- 项目详情页可以由结构化字段 + MDX 长文组合生成。

### 2.2 数据模型草案

Profile：

- name
- title
- targetRole
- summary
- contact
- resumePdfUrl
- avatarUrl

Experience：

- company
- position
- startDate
- endDate
- location
- summary
- responsibilities
- achievements
- metrics
- techTags
- relatedProjects
- visibility
- sortOrder

Project：

- name
- slug
- role
- startDate
- endDate
- summary
- background
- techStack
- responsibilities
- highlights
- challenges
- solutions
- metrics
- architecture
- demoModules
- interviewTalkingPoints
- visibility
- sortOrder

Skill：

- category
- name
- level
- description
- relatedExperiences
- relatedProjects

ModuleDemo：

- name
- slug
- demoType
- businessContext
- techPoints
- demoConfig
- relatedProject
- relatedExperience
- jdKeywords
- interviewValue
- talkingPoints
- apiBase
- visibility
- sortOrder

Article / CaseStudy：

- title
- slug
- summary
- content
- tags
- relatedProject

ProfileVariant：

- name
- sourceJd
- targetRole
- generatedSummary
- generatedIntroShort
- generatedIntroLong
- recommendedProjectOrder
- projectEmphasis
- skillEmphasis
- riskNotes
- createdAt

## 3. JD 智能适配器功能设计

### 3.1 用户流程

1. 用户进入 `/jd-adapter`。
2. 粘贴岗位 JD。
3. 选择目标输出：
   - 首页展示文案
   - 简历摘要
   - 项目页强调重点
   - 面试自我介绍
   - 技能匹配分析
4. 后端读取个人经历数据。
5. 后端调用 LLM 进行分析与生成。
6. 前端展示生成结果，并标记“有经历支撑”和“需要谨慎表达”的部分。
7. 用户保存为一个 Profile Variant。
8. 页面可以预览该 Variant 下的首页/项目页内容。

### 3.2 页面输出内容

岗位解析：

- 岗位方向
- 必备技术
- 加分技术
- 业务关键词
- 软素质要求
- 风险要求

匹配结果：

- 高匹配项：简历中有明确经历支撑。
- 中匹配项：相关但需要换表达角度。
- 低匹配项：缺少直接经历，不应强行包装。

生成内容：

- 20 秒个人简介
- 1 分钟自我介绍
- 3 分钟项目讲解稿
- 首页 Hero 文案
- 项目展示排序
- 每个项目的 JD 适配版摘要
- 面试官可能追问的问题

## 4. LLM 后端设计

推荐接口：

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
  "role": "Java 后端开发实习生",
  "keywords": ["Java", "Spring Boot", "MySQL", "Redis", "微服务"],
  "matchScore": 86,
  "summary": "...",
  "introShort": "...",
  "introLong": "...",
  "projectOrder": ["mine-education-system", "private-ai-platform"],
  "projectEmphasis": [
    {
      "projectSlug": "mine-education-system",
      "emphasis": "重点突出 Redis 缓存优化、学习状态流转、微服务接口开发",
      "supportedBy": ["Redis + 延迟任务", "Spring Cloud", "6 个核心接口"]
    }
  ],
  "riskNotes": [
    "JD 中提到高并发压测经验，当前简历没有明确压测数据，建议表达为具备缓存优化与性能排查意识。"
  ]
}
```

### 4.1 Prompt 原则

- 只能基于已有 Profile、Experience、Project、Skill 数据生成。
- 不允许编造不存在的公司、项目、指标和技术经验。
- 对缺少支撑的 JD 要求，要输出风险提示。
- 输出内容要适合中文技术面试，表达自然、具体、可追问。
- 每个生成结论最好带 `supportedBy` 字段，说明来自哪段经历。

### 4.2 技术实现

后端：

- Spring Boot 提供 JD 分析接口。
- LLM Provider 抽象层，后续可切换 OpenAI、通义千问、DeepSeek、智谱等。
- PromptTemplate 存储在代码或数据库中。
- 生成结果保存到 ProfileVariant 表。
- 对相同 JD 做 hash，支持结果缓存。

前端：

- `/jd-adapter` 页面负责输入 JD、展示分析结果。
- `/preview?variantId=xxx` 预览适配后的首页或项目页。
- 首页和项目页支持读取默认 Profile 或某个 ProfileVariant。
- JD Adapter 可以根据 ModuleDemo 的 `jdKeywords` 和 `techPoints` 推荐最适合展示的工作复现模块。

## 5. 页面如何动态替换

不是让 LLM 直接改页面代码，而是让 LLM 生成结构化内容，再由固定组件渲染。

可以动态替换的区域：

- 首页 Hero 的一句话定位。
- 首页核心亮点卡片排序。
- 项目列表排序。
- 项目卡片摘要。
- 项目详情页顶部的“为什么这个项目匹配该岗位”。
- 在线简历页的个人总结。
- 面试准备页的自我介绍和追问清单。

不建议动态替换的区域：

- 原始经历数据。
- 真实项目事实。
- 技术指标。
- 公司/学校/奖项等硬信息。

## 6. 面试中的价值

这个功能本身也可以成为项目亮点：

- 展示 LLM 应用不是简单聊天，而是结合结构化个人数据做场景化生成。
- 展示后端抽象能力：Provider 抽象、Prompt 管理、结果缓存、风险控制。
- 展示产品思维：同一份经历面对不同岗位需要不同表达。
- 展示工程边界：LLM 只能重组和表达，不能编造经历。

推荐项目介绍：

> 我把个人作品集做成了一个可扩展的经历资产库，项目和实习经历都以结构化数据维护。除此之外，我加入了 JD 智能适配功能：粘贴岗位描述后，系统会调用 LLM 分析岗位要求，并基于已有经历生成更匹配的个人介绍、项目排序和项目讲解重点。为了避免过度包装，我会让模型输出每个结论的经历支撑和风险提示。

## 7. 第一版实现范围

第一版只做最有价值的闭环：

- 后台或种子数据维护 Profile、Experience、Project、Skill。
- `/jd-adapter` 页面输入 JD。
- 后端调用 LLM 返回结构化 JSON。
- 保存 ProfileVariant。
- 首页支持根据 variantId 展示不同 Hero 文案和项目排序。
- 项目详情页顶部显示 JD 适配说明。

后续再增强：

- 多模型切换。
- Prompt 版本管理。
- 生成内容人工编辑。
- 对比多个 JD 的匹配度。
- 导出定制版 PDF 简历。
