INSERT INTO schema_version (version, description)
VALUES ('0.2.0', 'mysql content repository')
ON DUPLICATE KEY UPDATE description = 'mysql content repository';

INSERT INTO schema_version (version, description)
VALUES ('0.3.0', 'blog content module')
ON DUPLICATE KEY UPDATE description = 'blog content module';

INSERT INTO schema_version (version, description)
VALUES ('0.4.0', 'blog authoring and interaction module')
ON DUPLICATE KEY UPDATE description = 'blog authoring and interaction module';

INSERT INTO schema_version (version, description)
VALUES ('0.5.0', 'resume content management module')
ON DUPLICATE KEY UPDATE description = 'resume content management module';

DELETE FROM resume_section_item WHERE version_id IN (SELECT id FROM resume_version WHERE version_name = '默认草稿');
DELETE FROM resume_basic_info WHERE version_id IN (SELECT id FROM resume_version WHERE version_name = '默认草稿');
DELETE FROM resume_version WHERE version_name = '默认草稿';

DELETE FROM blog_like WHERE post_id IN (
  SELECT id FROM blog_post WHERE slug IN ('redis-video-progress-buffer', 'rag-sqlbot-agent-boundary', 'portfolio-docker-deploy-notes')
);
DELETE FROM blog_annotation WHERE post_id IN (
  SELECT id FROM blog_post WHERE slug IN ('redis-video-progress-buffer', 'rag-sqlbot-agent-boundary', 'portfolio-docker-deploy-notes')
);
DELETE FROM blog_comment WHERE post_id IN (
  SELECT id FROM blog_post WHERE slug IN ('redis-video-progress-buffer', 'rag-sqlbot-agent-boundary', 'portfolio-docker-deploy-notes')
);
DELETE FROM blog_post_tag WHERE post_id IN (
  SELECT id FROM blog_post WHERE slug IN ('redis-video-progress-buffer', 'rag-sqlbot-agent-boundary', 'portfolio-docker-deploy-notes')
);
DELETE FROM blog_post WHERE slug IN ('redis-video-progress-buffer', 'rag-sqlbot-agent-boundary', 'portfolio-docker-deploy-notes');
DELETE FROM interview_open_link;
DELETE FROM interview_question;
DELETE FROM interview_project_order;
DELETE FROM interview_guide;
DELETE FROM module_demo_talking_point;
DELETE FROM module_demo_tech;
DELETE FROM module_demo;
DELETE FROM project_evidence;
DELETE FROM project_responsibility;
DELETE FROM project_metric;
DELETE FROM project_tech;
DELETE FROM project;
DELETE FROM experience_highlight;
DELETE FROM experience;
DELETE FROM skill_item;
DELETE FROM skill_group;
DELETE FROM profile_tag;
DELETE FROM profile;

INSERT INTO profile (id, name, title, summary, email, education) VALUES
(1, '赵豪然', 'Java 后端开发', '山西大学软件工程本科在读，关注 Spring Boot、微服务、Redis 缓存优化、AI 应用接入与工程化部署。', 'itmucizhr@163.com', '山西大学 软件工程 本科');

INSERT INTO resume_version (version_name, status) VALUES
('默认草稿', 'DRAFT');

INSERT INTO resume_basic_info (version_id, name, title, summary, email, phone, location, education, github_url, website_url) VALUES
((SELECT id FROM resume_version WHERE version_name = '默认草稿' ORDER BY id DESC LIMIT 1), '赵豪然', 'Java 后端开发', '山西大学软件工程本科在读，关注 Spring Boot、微服务、Redis 缓存优化、AI 应用接入与工程化部署。', 'itmucizhr@163.com', '', '山西', '山西大学 软件工程 本科', '', '');

INSERT INTO resume_section_item (version_id, section_type, title, subtitle, period, summary, detail, tags, visible, sort_order) VALUES
((SELECT id FROM resume_version WHERE version_name = '默认草稿' ORDER BY id DESC LIMIT 1), 'SKILL', 'Java / Spring Boot', '后端开发', '', 'Java 后端与 Spring Boot 工程实践', 'Spring MVC\nMyBatis-Plus\n统一异常处理', '["Java","Spring Boot","Backend"]', 1, 1),
((SELECT id FROM resume_version WHERE version_name = '默认草稿' ORDER BY id DESC LIMIT 1), 'SKILL', 'Redis / MySQL', '数据与缓存', '', '缓存削峰、慢 SQL 排查与数据同步', 'Redis 缓存\nMySQL 查询优化\n幂等同步', '["Redis","MySQL"]', 1, 2),
((SELECT id FROM resume_version WHERE version_name = '默认草稿' ORDER BY id DESC LIMIT 1), 'INTERNSHIP', '山西恒山科技有限公司', 'Java 后端实习', '2025.12 - 至今', '参与产品中心系统后端开发、维护与迭代。', '完成 8 个功能点及接口调整\n定位并修复 5 类 60 个问题\n协助优化 2 条慢 SQL', '["Java","Spring Boot","问题排查"]', 1, 1),
((SELECT id FROM resume_version WHERE version_name = '默认草稿' ORDER BY id DESC LIMIT 1), 'PROJECT', '矿山教育系统', '学习服务后端开发', '项目实践', '企业培训场景下的微服务学习闭环系统。', '视频进度上报\nRedis 缓存同步\n学习状态流转', '["Spring Boot","Redis","MySQL"]', 1, 1),
((SELECT id FROM resume_version WHERE version_name = '默认草稿' ORDER BY id DESC LIMIT 1), 'PROJECT', 'MaxKB + SqlBot 私有化 AI 管理平台', 'AI 平台接入与部署联调', '项目实践', '面向企业内部的 AI 应用统一管理、SQL 智能问答与 RAG 接入平台。', '私有化部署\nSQL 智能问答验证\nRAG 入库与检索流程', '["Docker","RAG","SQL Bot"]', 1, 2),
((SELECT id FROM resume_version WHERE version_name = '默认草稿' ORDER BY id DESC LIMIT 1), 'ADVANTAGE', '工程化意识', '个人优势', '', '能把项目经历拆成可运行、可验证、可讲解的工程证据。', '关注接口设计、部署链路、异常兜底和面试表达。', '["工程化","表达"]', 1, 1);

INSERT INTO profile_tag (profile_id, tag, sort_order) VALUES
(1, 'Spring Boot', 1), (1, 'Spring Cloud', 2), (1, 'Redis', 3), (1, 'MySQL', 4),
(1, 'AI 应用', 5), (1, 'Docker', 6), (1, 'Jenkins', 7);

INSERT INTO skill_group (id, category, sort_order) VALUES
(1, 'Backend', 1), (2, 'Microservice', 2), (3, 'Data', 3), (4, 'AI Application', 4), (5, 'DevOps', 5);

INSERT INTO skill_item (group_id, name, sort_order) VALUES
(1, 'Java', 1), (1, 'Spring Boot', 2), (1, 'Spring MVC', 3), (1, 'MyBatis-Plus', 4), (1, '统一异常处理', 5),
(2, 'Spring Cloud', 1), (2, 'Gateway', 2), (2, 'OpenFeign', 3), (2, 'RabbitMQ', 4),
(3, 'MySQL', 1), (3, 'Redis', 2), (3, '慢 SQL 排查', 3), (3, '缓存优化', 4),
(4, 'Spring AI', 1), (4, 'LangGraph', 2), (4, 'RAG', 3), (4, 'MaxKB', 4), (4, 'SqlBot', 5),
(5, 'Docker', 1), (5, 'Nginx', 2), (5, 'Jenkins', 3), (5, 'Linux', 4), (5, 'Maven', 5), (5, 'Git', 6);

INSERT INTO experience (id, company, position, period, sort_order) VALUES
(1, '山西恒山科技有限公司', 'Java 后端实习', '2025.12 - 至今', 1);

INSERT INTO experience_highlight (experience_id, highlight, sort_order) VALUES
(1, '参与产品中心系统后端开发、维护与迭代，完成 8 个功能点及接口调整。', 1),
(1, '定位并修复接口异常、参数错误、业务逻辑缺陷等 5 类 60 个问题。', 2),
(1, '协助优化 2 条慢 SQL，将部分查询接口响应从 2s+ 降至 800ms 左右。', 3),
(1, '参与邮件告警、特殊格式文件解析、AI 功能模块接入与交付文档编写。', 4);

INSERT INTO project (id, project_index, slug, name, summary, role, sort_order) VALUES
(1, '01', 'mine-education-system', '矿山教育系统', '企业培训场景下的微服务学习闭环系统。', '学习服务后端开发', 1),
(2, '02', 'private-ai-platform', 'MaxKB + SqlBot 私有化 AI 管理平台', '面向企业内部的 AI 应用统一管理、SQL 智能问答与 RAG 接入平台。', 'AI 平台接入与部署联调', 2);

INSERT INTO project_tech (project_id, tech, sort_order) VALUES
(1, 'Spring Boot', 1), (1, 'Spring Cloud', 2), (1, 'Redis', 3), (1, 'MySQL', 4), (1, 'RabbitMQ', 5),
(2, 'Docker', 1), (2, 'Nginx', 2), (2, 'RAG', 3), (2, 'SQL Bot', 4), (2, 'LangGraph', 5);

INSERT INTO project_metric (project_id, metric, sort_order) VALUES
(1, '重复写入 -70%', 1), (1, '响应 1.5s -> 500ms', 2),
(2, '10+ 问答验证', 1), (2, '入库效率 +30%', 2);

INSERT INTO project_responsibility (project_id, responsibility, sort_order) VALUES
(1, '参与学习服务表结构和接口开发', 1),
(1, '实现视频进度上报与学习状态流转', 2),
(1, '设计 Redis + 延迟任务缓存同步方案', 3),
(2, '参与私有化部署和服务联调', 1),
(2, '验证 SQL 智能问答链路', 2),
(2, '参与 RAG 入库与检索流程优化', 3);

INSERT INTO project_evidence (project_id, problem, solution, result, sort_order) VALUES
(1, '视频播放进度高频上报导致数据库重复写入压力大。', '播放中优先写 Redis，完播或延迟任务触发后同步 MySQL。', '重复写入减少约 70%，部分接口响应从 1.5s 降至 500ms 左右。', 1),
(2, '企业内部 AI 能力需要本地化部署、统一管理和数据隔离。', '接入 MaxKB、SqlBot、RAG 与基础部署配置，形成可验证平台链路。', '完成 3 个核心服务联调，验证 10+ 条典型 SQL 问答样例。', 1);

INSERT INTO module_demo (id, slug, name, title, demo_type, project, summary, api_base, sort_order) VALUES
(1, 'video-learning', 'Video Learning', '视频播放学习链路', 'video-learning', '矿山教育系统', '复现视频播放、进度上报、Redis 缓存、完播同步和状态流转。', '/api/lab/video-learning', 1),
(2, 'cache-sync', 'Cache Sync', 'Redis 学习记录缓存同步', 'cache-sync', '矿山教育系统', '展示高频写入削峰、延迟同步、重复写入减少和响应时间优化。', '/api/lab/video-learning', 2),
(3, 'agent-workflow', 'Agent Workflow', 'Agent 工作流编排', 'agent-workflow', '私有化 AI 管理平台', '模拟意图识别、工具路由、RAG/SQL 查询、模型回答和结果校验。', '/api/lab/agent-workflow', 3),
(4, 'sql-bot', 'SQL Bot', 'SQL 智能问答', 'sql-bot', '私有化 AI 管理平台', '复现自然语言问题到 SQL 生成、校验、查询与结果解释。', '/api/lab/agent-workflow', 4),
(5, 'rag-flow', 'RAG Flow', 'RAG 知识库检索', 'rag-flow', '私有化 AI 管理平台', '展示文档切块、向量写入、Top-K 召回和上下文回答生成。', '/api/lab/agent-workflow', 5);

INSERT INTO module_demo_tech (module_id, tech, sort_order) VALUES
(1, 'Video Event', 1), (1, 'Redis', 2), (1, 'MySQL', 3), (1, 'State Flow', 4),
(2, 'Redis', 1), (2, 'Delayed Task', 2), (2, 'Idempotent', 3),
(3, 'Agent', 1), (3, 'Tool Calling', 2), (3, 'RAG', 3), (3, 'SQL Bot', 4),
(4, 'LLM', 1), (4, 'SQL Safety', 2), (4, 'Metadata', 3),
(5, 'Chunking', 1), (5, 'Vector Search', 2), (5, 'Retrieval', 3);

INSERT INTO module_demo_talking_point (module_id, talking_point, sort_order) VALUES
(1, '为什么播放中先写缓存', 1), (1, '完播同步如何降低重复写入', 2), (1, '状态流转如何避免脏数据', 3),
(2, '高频写入削峰', 1), (2, '幂等同步', 2), (2, 'Redis 与 MySQL 快照对比', 3),
(3, '工具路由边界', 1), (3, 'RAG 与 SQL Bot 分工', 2), (3, '模型回答结果校验', 3),
(4, 'SQL 安全校验', 1), (4, '元数据约束', 2), (4, '问答结果解释', 3),
(5, '切块策略', 1), (5, 'Top-K 召回', 2), (5, '上下文组装', 3);

INSERT INTO interview_guide (id, short_intro) VALUES
(1, '我是赵豪然，山西大学软件工程本科在读，主攻 Java 后端开发，重点实践过微服务业务系统、Redis 缓存优化和 AI 应用工程化接入。');

INSERT INTO interview_project_order (guide_id, item_value, sort_order) VALUES
(1, '矿山教育系统', 1), (1, '视频播放学习链路 Demo', 2), (1, 'MaxKB + SqlBot 私有化 AI 管理平台', 3);

INSERT INTO interview_question (guide_id, item_value, sort_order) VALUES
(1, '为什么视频进度上报不直接写 MySQL？', 1),
(1, 'Redis 缓存同步如何处理重复上报和完播状态？', 2),
(1, 'AI 平台接入中 RAG、SQL Bot 和 Agent 的边界是什么？', 3);

INSERT INTO interview_open_link (guide_id, item_value, sort_order) VALUES
(1, '/projects/mine-education-system', 1), (1, '/lab/video-learning', 2), (1, '/jd-adapter', 3);

INSERT INTO blog_post (id, slug, title, excerpt, content, category, status, published_at, read_minutes, sort_order) VALUES
(1, 'redis-video-progress-buffer', '为什么视频进度上报不应该直接写 MySQL', '从视频学习场景出发，记录我对高频写入、缓存削峰、完播同步和幂等状态流转的理解。', '在视频学习场景里，播放进度会随着 timeupdate 高频触发。如果每一次进度变化都直接写 MySQL，数据库会承担大量重复写入，真实业务价值却很低。\n\n我的处理思路是把播放中的临时状态放到 Redis，按用户、课程、视频维度维护最新进度和写入次数。只有在完播、退出学习或延迟任务触发时，才把最终状态同步到 MySQL。\n\n这个方案的关键不是简单加缓存，而是把数据分为过程态和结果态。过程态追求低延迟和可覆盖，结果态追求准确和可追溯。面试里讲这段时，我会重点解释幂等 key、完播优先级和异常恢复。', '后端实践', 'published', '2026-06-03 21:00:00', 5, 1),
(2, 'rag-sqlbot-agent-boundary', 'RAG、SQL Bot 和 Agent 的职责边界', '总结私有化 AI 平台接入时，对检索增强、结构化查询和工具编排三类能力的拆分理解。', 'AI 应用工程化时，最容易把 RAG、SQL Bot 和 Agent 混成一个概念。但落到系统设计里，它们应该承担不同职责。\n\nRAG 更适合处理非结构化知识，比如制度文档、产品说明和操作手册。SQL Bot 更适合回答结构化数据问题，比如统计、筛选、趋势查询。Agent 则不应该替代所有能力，它更像调度层，负责识别意图、选择工具、串联步骤并校验结果。\n\n我认为可靠的 AI 应用不是让模型直接做一切，而是把模型放在受约束的工程链路里：输入解析、工具调用、权限边界、结果校验和可观测日志都要明确。', 'AI学习', 'published', '2026-06-03 21:10:00', 4, 2),
(3, 'portfolio-docker-deploy-notes', '把作品集做成 Docker 全栈项目的复盘', '记录这个个人作品集从前端页面到 Spring Boot API、MySQL、Nginx 反代和 Docker Compose 的工程化思路。', '个人作品集如果只是静态页面，面试官只能看到设计效果；如果它能真的跑起来，就可以展示完整工程链路。\n\n这个项目采用 Next.js 前端、Spring Boot 后端、MySQL 内容库、Redis 运行时缓存、Nginx 统一入口和 Docker Compose 编排。浏览器只访问一个 3000 端口，内部再由 Nginx 转发到 web 和 server。\n\n这个过程里踩过一个典型问题：普通 Maven jar 没有 Spring Boot 可执行 manifest，容器里 java -jar 会失败。解决方式是执行 spring-boot:repackage，让 jar 带上启动入口和 BOOT-INF 依赖结构。这个问题很适合在面试里展示排障过程。', '工程部署', 'published', '2026-06-03 21:20:00', 4, 3);

INSERT INTO blog_post_tag (post_id, tag, sort_order) VALUES
(1, 'Redis', 1), (1, 'MySQL', 2), (1, '缓存同步', 3),
(2, 'RAG', 1), (2, 'SQL Bot', 2), (2, 'Agent', 3),
(3, 'Docker', 1), (3, 'Nginx', 2), (3, 'Spring Boot', 3);

INSERT INTO blog_comment (post_id, author, content, created_at) VALUES
(1, '面试官视角', '这一篇适合在讲 Redis 缓存削峰时打开，能补充为什么不是所有状态都应该直接落库。', '2026-06-03 21:30:00'),
(3, '部署复盘', 'Docker 化这块可以继续补 Jenkins Pipeline 和健康检查截图。', '2026-06-03 21:40:00');

INSERT INTO blog_annotation (post_id, anchor_text, note, created_at) VALUES
(1, '过程态追求低延迟和可覆盖', '这里可以作为架构取舍的核心旁注：缓存负责高频过程，数据库负责最终事实。', '2026-06-03 21:35:00'),
(2, '受约束的工程链路', '面试里可以展开权限、工具调用日志和结果校验。', '2026-06-03 21:45:00');

INSERT INTO blog_like (post_id, client_key, created_at) VALUES
(1, 'seed-redis-1', '2026-06-03 21:36:00'),
(1, 'seed-redis-2', '2026-06-03 21:37:00'),
(2, 'seed-ai-1', '2026-06-03 21:46:00'),
(3, 'seed-devops-1', '2026-06-03 21:47:00');
