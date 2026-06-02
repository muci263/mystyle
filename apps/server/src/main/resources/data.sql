INSERT INTO schema_version (version, description)
VALUES ('0.2.0', 'mysql content repository')
ON DUPLICATE KEY UPDATE description = 'mysql content repository';

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
