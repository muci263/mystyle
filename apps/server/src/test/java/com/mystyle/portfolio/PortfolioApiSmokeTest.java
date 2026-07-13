package com.mystyle.portfolio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

import com.mystyle.portfolio.analytics.AnalyticsController;
import com.mystyle.portfolio.analytics.AnalyticsService;
import com.mystyle.portfolio.blog.BlogController;
import com.mystyle.portfolio.common.GlobalExceptionHandler;
import com.mystyle.portfolio.config.CorsConfig;
import com.mystyle.portfolio.content.JdbcPortfolioContentRepository;
import com.mystyle.portfolio.content.PortfolioContentService;
import com.mystyle.portfolio.health.HealthController;
import com.mystyle.portfolio.health.HealthService;
import com.mystyle.portfolio.jd.JdAnalysisService;
import com.mystyle.portfolio.jd.JdController;
import com.mystyle.portfolio.knowledge.KnowledgeGraphController;
import com.mystyle.portfolio.knowledge.KnowledgeGraphSmartService;
import com.mystyle.portfolio.llm.LlmController;
import com.mystyle.portfolio.llm.LlmService;
import com.mystyle.portfolio.moduleDemo.AgentWorkflowController;
import com.mystyle.portfolio.moduleDemo.AgentWorkflowService;
import com.mystyle.portfolio.moduleDemo.ModuleDemoController;
import com.mystyle.portfolio.moduleDemo.VideoLearningController;
import com.mystyle.portfolio.moduleDemo.VideoLearningService;
import com.mystyle.portfolio.profile.ProfileController;
import com.mystyle.portfolio.profile.ResumeController;
import com.mystyle.portfolio.project.ProjectController;
import com.mystyle.portfolio.resume.JdbcResumeAdminRepository;
import com.mystyle.portfolio.resume.ResumeAdminController;
import com.mystyle.portfolio.resume.ResumeAdminService;
import com.mystyle.portfolio.resume.ResumePublicController;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortfolioApiSmokeTest {
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
    dataSource.setDriverClass(org.h2.Driver.class);
    dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
        new ClassPathResource("schema.sql"),
        new ClassPathResource("data.sql"));
    populator.setSqlScriptEncoding("UTF-8");
    populator.execute(dataSource);

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    ObjectMapper objectMapper = new ObjectMapper();
    PortfolioContentService contentService = new PortfolioContentService(new JdbcPortfolioContentRepository(jdbcTemplate));
    LlmService llmService = new LlmService(objectMapper, "", "https://api.minimaxi.com/v1", "MiniMax-M2.7", "MiniMax-Text-01");
    ResumeAdminService resumeAdminService = new ResumeAdminService(new JdbcResumeAdminRepository(jdbcTemplate, objectMapper), objectMapper, llmService);
    KnowledgeGraphSmartService knowledgeGraphSmartService = new KnowledgeGraphSmartService(contentService, llmService);
    JdAnalysisService jdAnalysisService = new JdAnalysisService(contentService, llmService);
    VideoLearningService videoLearningService = new VideoLearningService();
    AgentWorkflowService agentWorkflowService = new AgentWorkflowService(llmService);
    AnalyticsService analyticsService = new AnalyticsService();
    CorsConfig corsConfig = new CorsConfig("http://localhost:3000,http://127.0.0.1:3000");

    mockMvc = MockMvcBuilders.standaloneSetup(
            new HealthController(new HealthService(jdbcTemplate, llmService)),
            new ProfileController(contentService),
            new ResumeController(contentService),
            new ProjectController(contentService),
            new ModuleDemoController(contentService),
            new BlogController(contentService),
            new KnowledgeGraphController(contentService, knowledgeGraphSmartService),
            new ResumeAdminController(resumeAdminService),
            new ResumePublicController(resumeAdminService),
            new JdController(jdAnalysisService),
            new LlmController(llmService, objectMapper),
            new VideoLearningController(videoLearningService),
            new AgentWorkflowController(agentWorkflowService, objectMapper),
            new AnalyticsController(analyticsService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .addFilters(corsConfig.corsFilter())
        .build();
  }

  @Test
  void corsPreflightShouldAllowConfiguredFrontendOrigin() throws Exception {
    mockMvc.perform(options("/public/home")
            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  void publicContentEndpointsShouldReturnStructuredAssets() throws Exception {
    mockMvc.perform(get("/public/home"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.profile.name").value("赵豪然"))
        .andExpect(jsonPath("$.data.featuredProjects[0].slug").value("mine-education-system"))
        .andExpect(jsonPath("$.data.knowledgeGraph.nodes[0].nodeKey").value("me"));

    mockMvc.perform(get("/public/projects/mine-education-system"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.evidence[0].problem").exists());

    mockMvc.perform(get("/public/module-demos").param("tech", "Redis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].slug").value("video-learning"));
  }

  @Test
  void knowledgeGraphShouldExposeBlogChildrenAndSupportCrud() throws Exception {
    mockMvc.perform(get("/public/knowledge-graph"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nodes[*].nodeKey", hasItem("section-blog")))
        .andExpect(jsonPath("$.data.nodes[*].nodeKey", hasItem("blog-redis-video-progress-buffer")))
        .andExpect(jsonPath("$.data.edges[*].toNodeKey", hasItem("blog-redis-video-progress-buffer")))
        .andExpect(jsonPath("$.data.edges[*].relationType", hasItem("CONTAINS")))
        .andExpect(jsonPath("$.data.edges[*].visible", hasItem(false)));

    mockMvc.perform(post("/admin/knowledge-graph/nodes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
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
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nodeKey").value("blog-test-note"));

    mockMvc.perform(post("/admin/knowledge-graph/nodes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "nodeKey": "blog-wrong-level",
                  "label": "错误层级博客",
                  "nodeType": "BLOG",
                  "level": 0,
                  "summary": "BLOG 必须是三级内容节点",
                  "content": "用于校验层级规则。",
                  "tags": ["Blog"],
                  "href": "/blog/wrong-level",
                  "sourceType": "BLOG",
                  "sourceSlug": "wrong-level",
                  "x": 0,
                  "y": 0,
                  "z": 0,
                  "visible": true,
                  "sortOrder": 1001
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("节点层级不匹配：BLOG 必须为 三级"));

    mockMvc.perform(put("/admin/knowledge-graph/nodes/blog-test-note")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "nodeKey": "blog-test-note",
                  "label": "测试博客节点更新",
                  "nodeType": "BLOG",
                  "level": 2,
                  "summary": "更新后的测试节点",
                  "content": "更新后的悬停展示内容。",
                  "tags": ["Updated", "Blog"],
                  "href": "/blog/test-note",
                  "sourceType": "BLOG",
                  "sourceSlug": "test-note",
                  "x": 1.3,
                  "y": -3.5,
                  "z": 0.1,
                  "visible": true,
                  "sortOrder": 1000
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.label").value("测试博客节点更新"))
        .andExpect(jsonPath("$.data.content").value("更新后的悬停展示内容。"));

    mockMvc.perform(post("/admin/knowledge-graph/edges")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fromNodeKey": "module-agent-workflow",
                  "toNodeKey": "me",
                  "relationType": "RELATED",
                  "visible": false,
                  "sortOrder": 1000
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("不能跨级连接：三级 节点不能直接连接 一级 节点"));

    String tertiaryEdgeResponse = mockMvc.perform(post("/admin/knowledge-graph/edges")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fromNodeKey": "blog-test-note",
                  "toNodeKey": "skill-redis",
                  "relationType": "EXPLAINS",
                  "visible": false,
                  "sortOrder": 1001
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fromNodeKey").value("blog-test-note"))
        .andExpect(jsonPath("$.data.toNodeKey").value("skill-redis"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String edgeResponse = mockMvc.perform(post("/admin/knowledge-graph/edges")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "fromNodeKey": "section-blog",
                  "toNodeKey": "blog-test-note",
                  "relationType": "CONTAINS",
                  "visible": true,
                  "sortOrder": 1000
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fromNodeKey").value("section-blog"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    mockMvc.perform(put("/admin/knowledge-graph/nodes/blog-test-note")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "nodeKey": "blog-test-note",
                  "label": "测试博客节点错误改层级",
                  "nodeType": "SECTION",
                  "level": 1,
                  "summary": "不能在已有三级关系时改成二级栏目",
                  "content": "用于校验已有关系保护。",
                  "tags": ["Section"],
                  "href": "",
                  "sourceType": "MANUAL",
                  "sourceSlug": "",
                  "x": 1.3,
                  "y": -3.5,
                  "z": 0.1,
                  "visible": true,
                  "sortOrder": 1000
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("节点层级变更会破坏已有关系")));

    long tertiaryEdgeId = tertiaryEdgeResponse.contains("\"id\":") ? Long.parseLong(tertiaryEdgeResponse.replaceAll(".*\\\"id\\\":(\\d+).*", "$1")) : 0;
    mockMvc.perform(delete("/admin/knowledge-graph/edges/{edgeId}", tertiaryEdgeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value("DELETED"));

    long edgeId = edgeResponse.contains("\"id\":") ? Long.parseLong(edgeResponse.replaceAll(".*\\\"id\\\":(\\d+).*", "$1")) : 0;
    mockMvc.perform(delete("/admin/knowledge-graph/edges/{edgeId}", edgeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value("DELETED"));

    mockMvc.perform(delete("/admin/knowledge-graph/nodes/blog-test-note"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value("DELETED"));
  }

  @Test
  void aiKnowledgeGraphEndpointsShouldFailWithoutRealProvider() throws Exception {
    mockMvc.perform(get("/llm/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.provider").value("minimax-chat-completions"))
        .andExpect(jsonPath("$.data.configured").value(false))
        .andExpect(jsonPath("$.data.mode").value("missing-api-key"));

    mockMvc.perform(post("/admin/knowledge-graph/nodes/smart-create")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "label": "智能建边测试",
                  "nodeType": "BLOG",
                  "summary": "Redis 与 MySQL 缓存同步的测试节点",
                  "tags": ["Redis", "MySQL"],
                  "visible": true
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(50310));

    mockMvc.perform(post("/admin/knowledge-graph/nodes/{nodeKey}/auto-relate", "blog-redis-video-progress-buffer"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(50310));

    mockMvc.perform(post("/admin/knowledge-graph/nodes/{nodeKey}/auto-relate", "blog-redis-video-progress-buffer")
            .param("allowFallback", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.provider").value("local-template"));

    mockMvc.perform(get("/admin/knowledge-graph/nodes/{nodeKey}/auto-relate", "blog-redis-video-progress-buffer"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value(40500));
  }

  @Test
  void blogEndpointsShouldReturnPublishedPostsAndDetail() throws Exception {
    mockMvc.perform(get("/public/blog-posts/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("实习心得"))
        .andExpect(jsonPath("$.data[0].code").value("INTERNSHIP"));

    mockMvc.perform(get("/public/blog-posts").param("tag", "Redis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data[0].slug").value("redis-video-progress-buffer"))
        .andExpect(jsonPath("$.data[0].likeCount").value(2));

    mockMvc.perform(get("/public/blog-posts/portfolio-docker-deploy-notes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("把作品集做成 Docker 全栈项目的复盘"))
        .andExpect(jsonPath("$.data.tags[0]").value("Docker"));

    mockMvc.perform(post("/public/blog-posts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "一次接口幂等性的复盘",
                  "excerpt": "记录一次从重复提交到幂等 key 设计的技术复盘。",
                  "content": "背景：接口会被重复调用。\\n\\n方案：用业务唯一键和状态流转兜底。",
                  "category": "INTERNSHIP",
                  "tags": ["Idempotent", "API"],
                  "readMinutes": 3
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.slug").value("post"))
        .andExpect(jsonPath("$.data.category").value("实习心得"));

    mockMvc.perform(put("/public/blog-posts/post")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "一次接口幂等性的复盘",
                  "excerpt": "更新后的接口幂等复盘摘要。",
                  "content": "背景：接口会被重复调用。\\n\\n方案：用业务唯一键、状态流转和重试记录兜底。",
                  "category": "AI学习",
                  "tags": ["Idempotent", "AI学习"],
                  "readMinutes": 4
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.category").value("AI学习"))
        .andExpect(jsonPath("$.data.tags[1]").value("AI学习"));

    mockMvc.perform(post("/public/blog-posts/redis-video-progress-buffer/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "author": "tester",
                  "content": "这条评论来自 smoke test"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.author").value("tester"));

    mockMvc.perform(post("/public/blog-posts/redis-video-progress-buffer/annotations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "anchorText": "过程态",
                  "note": "旁注可以沉淀面试讲法"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.anchorText").value("过程态"));

    mockMvc.perform(put("/public/blog-posts/redis-video-progress-buffer/annotations/3")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "anchorText": "过程态更新",
                  "note": "旁注已更新"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.anchorText").value("过程态更新"))
        .andExpect(jsonPath("$.data.note").value("旁注已更新"));

    mockMvc.perform(delete("/public/blog-posts/redis-video-progress-buffer/annotations/3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.annotationCount").value(1));

    mockMvc.perform(post("/public/blog-posts/redis-video-progress-buffer/likes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.likeCount").value(3));
  }

  @Test
  void resumeAdminShouldManageDraftUploadAndPublish() throws Exception {
    mockMvc.perform(get("/admin/resume/draft"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.version.status").value("DRAFT"))
        .andExpect(jsonPath("$.data.basicInfo.name").value("赵豪然"))
        .andExpect(jsonPath("$.data.sections.SKILL[0].title").value("Java / Spring Boot"));

    String textResumeBase64 = Base64.getEncoder().encodeToString(
        "赵豪然\nJava 后端开发\n项目经历\nRedis 视频进度缓存".getBytes(StandardCharsets.UTF_8));
    mockMvc.perform(post("/admin/resume/uploads/extract-text")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "filename": "resume.txt",
                  "contentType": "text/plain",
                  "contentBase64": "%s"
                }
                """.formatted(textResumeBase64)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.rawText").value("赵豪然\nJava 后端开发\n项目经历\nRedis 视频进度缓存"));

    ByteArrayOutputStream docxOutput = new ByteArrayOutputStream();
    try (XWPFDocument document = new XWPFDocument()) {
      document.createParagraph().createRun().setText("赵豪然 DOCX 简历");
      document.createParagraph().createRun().setText("Spring Boot Redis MySQL");
      document.write(docxOutput);
    }
    String docxResumeBase64 = Base64.getEncoder().encodeToString(docxOutput.toByteArray());
    mockMvc.perform(post("/admin/resume/uploads/extract-text")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "filename": "resume.docx",
                  "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                  "contentBase64": "%s"
                }
                """.formatted(docxResumeBase64)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.rawText").value(org.hamcrest.Matchers.containsString("赵豪然 DOCX 简历")))
        .andExpect(jsonPath("$.data.rawText").value(org.hamcrest.Matchers.containsString("Spring Boot Redis MySQL")));

    ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);
      try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.newLineAtOffset(72, 720);
        contentStream.showText("Resume PDF Java Redis MySQL");
        contentStream.endText();
      }
      document.save(pdfOutput);
    }
    String pdfResumeBase64 = Base64.getEncoder().encodeToString(pdfOutput.toByteArray());
    mockMvc.perform(post("/admin/resume/uploads/extract-text")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "filename": "resume.pdf",
                  "contentType": "application/pdf",
                  "contentBase64": "%s"
                }
                """.formatted(pdfResumeBase64)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.rawText").value(org.hamcrest.Matchers.containsString("Resume PDF Java Redis MySQL")));

    mockMvc.perform(put("/admin/resume/basic-info")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "赵豪然",
                  "title": "Java 后端开发 / AI 工程化",
                  "summary": "用于测试的可编辑个人信息。",
                  "email": "itmucizhr@163.com",
                  "phone": "",
                  "location": "山西",
                  "education": "山西大学 软件工程 本科",
                  "githubUrl": "",
                  "websiteUrl": ""
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Java 后端开发 / AI 工程化"));

    mockMvc.perform(post("/admin/resume/sections/AWARD/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "校级奖学金",
                  "subtitle": "获奖经历",
                  "period": "2024",
                  "summary": "用于测试的获奖条目",
                  "detail": "可编辑、可排序、可隐藏",
                  "tags": ["奖学金"],
                  "visible": true,
                  "sortOrder": 1
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sectionType").value("AWARD"))
        .andExpect(jsonPath("$.data.title").value("校级奖学金"));

    mockMvc.perform(get("/admin/resume/sections/AWARD/items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].title").value("校级奖学金"));

    mockMvc.perform(post("/admin/resume/uploads/parse")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "filename": "resume.txt",
                  "contentType": "text/plain",
                  "content": "赵豪然\\nitmucizhr@163.com\\n技术能力\\nJava Spring Boot Redis MySQL\\n项目经历\\n矿山教育系统\\n视频进度上报与 Redis 缓存同步\\n个人优势\\n工程化意识强"
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(50310));

    String taskResponse = mockMvc.perform(post("/admin/resume/uploads/parse")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "filename": "resume.txt",
                  "contentType": "text/plain",
                  "content": "赵豪然\\nitmucizhr@163.com\\n山西大学 软件工程 本科\\n技术能力\\nJava Spring Boot Redis MySQL\\n项目经历\\n矿山教育系统\\n视频进度上报与 Redis 缓存同步\\n个人优势\\n工程化意识强",
                  "allowFallback": true
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PARSED"))
        .andExpect(jsonPath("$.data.errorMessage").value("已生成可确认的结构化草稿，请确认后写入草稿"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    long taskId = taskResponse.contains("\"id\":") ? Long.parseLong(taskResponse.replaceAll(".*\\\"id\\\":(\\d+).*", "$1")) : 0;
    mockMvc.perform(post("/admin/resume/uploads/{taskId}/confirm", taskId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.basicInfo.name").value("赵豪然"));

    mockMvc.perform(post("/admin/resume/publish"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

    mockMvc.perform(get("/public/resume-content"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.version.status").value("PUBLISHED"))
        .andExpect(jsonPath("$.data.basicInfo.name").value("赵豪然"))
        .andExpect(jsonPath("$.data.sections.PROJECT[0].title").value("矿山教育系统"));

    mockMvc.perform(get("/admin/resume/versions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].status").exists());
  }

  @Test
  void jdAnalyzeShouldFailWithoutRealProvider() throws Exception {
    mockMvc.perform(post("/llm/resume/optimize")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "resumeText": "赵豪然，Java 后端开发，做过 Redis 与 Spring Boot 项目。",
                  "jdText": "Java 后端实习，要求 Spring Boot、Redis、MySQL",
                  "targetRole": "Java 后端开发实习生"
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(50310));

    mockMvc.perform(post("/llm/resume/optimize")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "resumeText": "赵豪然，Java 后端开发，做过 Redis 与 Spring Boot 项目。",
                  "jdText": "Java 后端实习，要求 Spring Boot、Redis、MySQL",
                  "targetRole": "Java 后端开发实习生",
                  "allowFallback": true
                }
        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.provider").value("local-template"))
        .andExpect(jsonPath("$.data.generatedResumeMarkdown").exists())
        .andExpect(jsonPath("$.data.riskNotes[0]").value("这是本地模板生成的辅助结果，请人工确认后使用。"));

    mockMvc.perform(post("/llm/interview/mock")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "resumeText": "赵豪然，Java 后端开发，做过 Redis 与 Spring Boot 项目。",
                  "jdText": "Java 后端实习，要求 Spring Boot、Redis、MySQL",
                  "targetRole": "Java 后端开发实习生"
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(50310));

    mockMvc.perform(post("/llm/interview/mock")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "resumeText": "赵豪然，Java 后端开发，做过 Redis 与 Spring Boot 项目。",
                  "jdText": "Java 后端实习，要求 Spring Boot、Redis、MySQL",
                  "targetRole": "Java 后端开发实习生",
                  "allowFallback": true
                }
        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.provider").value("local-template"))
        .andExpect(jsonPath("$.data.questions[0].question").exists())
        .andExpect(jsonPath("$.data.riskNotes[0]").value("这是本地模板生成的辅助结果，请人工确认后使用。"));

    mockMvc.perform(post("/jd/analyze")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "jd": "Java 后端实习，要求 Spring Boot、Redis、MySQL、微服务，有 AI 或 RAG 经验加分",
                  "variantName": "Java 后端实习岗位"
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(50310));

    mockMvc.perform(post("/jd/analyze")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "jd": "Java 后端实习，要求 Spring Boot、Redis、MySQL、微服务，有 AI 或 RAG 经验加分",
                  "variantName": "Java 后端实习岗位",
                  "allowFallback": true
                }
        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.provider").value("local-template"))
        .andExpect(jsonPath("$.data.riskNotes[0]").value("这是本地模板生成的辅助结果，请人工确认后使用。"));
  }

  @Test
  void videoLearningShouldExposeCacheAndDatabaseSnapshots() throws Exception {
    mockMvc.perform(post("/lab/video-learning/reset"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.writeCount").value(0));

    mockMvc.perform(post("/lab/video-learning/progress")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userId": "interviewer",
                  "courseId": "course-java",
                  "lessonId": "lesson-redis",
                  "currentSecond": 42,
                  "durationSecond": 120
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.redisRecord.progressPercent").value(35))
        .andExpect(jsonPath("$.data.learningStatus").value("IN_PROGRESS"));

    mockMvc.perform(post("/lab/video-learning/complete"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.mysqlRecord.learningStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.data.learningStatus").value("COMPLETED"));
  }

  @Test
  void agentWorkflowShouldFailWithoutRealProviderAndAnalyticsShouldAcceptDemoRequests() throws Exception {
    mockMvc.perform(get("/lab/agent-workflow/templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].templateId").value("rag-question"));

    mockMvc.perform(post("/lab/agent-workflow/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "question": "如何解释 RAG 和 SQL Bot 的边界？",
                  "templateId": "sql-bot"
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(50310));

    mockMvc.perform(post("/analytics/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "eventType": "module_open",
                  "path": "/lab/video-learning",
                  "payload": { "source": "smoke-test" }
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
  }
}
