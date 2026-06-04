package com.mystyle.portfolio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.mystyle.portfolio.moduleDemo.AgentWorkflowController;
import com.mystyle.portfolio.moduleDemo.AgentWorkflowService;
import com.mystyle.portfolio.moduleDemo.ModuleDemoController;
import com.mystyle.portfolio.moduleDemo.VideoLearningController;
import com.mystyle.portfolio.moduleDemo.VideoLearningService;
import com.mystyle.portfolio.profile.ProfileController;
import com.mystyle.portfolio.profile.ResumeController;
import com.mystyle.portfolio.project.ProjectController;
import java.util.UUID;
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
    PortfolioContentService contentService = new PortfolioContentService(new JdbcPortfolioContentRepository(jdbcTemplate));
    JdAnalysisService jdAnalysisService = new JdAnalysisService(contentService);
    VideoLearningService videoLearningService = new VideoLearningService();
    AgentWorkflowService agentWorkflowService = new AgentWorkflowService();
    AnalyticsService analyticsService = new AnalyticsService();
    CorsConfig corsConfig = new CorsConfig("http://localhost:3000,http://127.0.0.1:3000");

    mockMvc = MockMvcBuilders.standaloneSetup(
            new HealthController(new HealthService(jdbcTemplate)),
            new ProfileController(contentService),
            new ResumeController(contentService),
            new ProjectController(contentService),
            new ModuleDemoController(contentService),
            new BlogController(contentService),
            new JdController(jdAnalysisService),
            new VideoLearningController(videoLearningService),
            new AgentWorkflowController(agentWorkflowService),
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
        .andExpect(jsonPath("$.data.featuredProjects[0].slug").value("mine-education-system"));

    mockMvc.perform(get("/public/projects/mine-education-system"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.evidence[0].problem").exists());

    mockMvc.perform(get("/public/module-demos").param("tech", "Redis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].slug").value("video-learning"));
  }

  @Test
  void blogEndpointsShouldReturnPublishedPostsAndDetail() throws Exception {
    mockMvc.perform(get("/public/blog-posts").param("tag", "Redis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data[0].slug").value("redis-video-progress-buffer"));

    mockMvc.perform(get("/public/blog-posts/portfolio-docker-deploy-notes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("把作品集做成 Docker 全栈项目的复盘"))
        .andExpect(jsonPath("$.data.tags[0]").value("Docker"));
  }

  @Test
  void jdAnalyzeShouldCreateRecommendationsAndVariant() throws Exception {
    String response = mockMvc.perform(post("/jd/analyze")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "jd": "Java 后端实习，要求 Spring Boot、Redis、MySQL、微服务，有 AI 或 RAG 经验加分",
                  "variantName": "Java 后端实习岗位"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.matchScore").value(96))
        .andExpect(jsonPath("$.data.projectRecommendations[0].slug").value("mine-education-system"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    long analysisId = response.contains("\"analysisId\":1") ? 1 : 0;
    mockMvc.perform(post("/jd/analyses/{analysisId}/variant", analysisId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("MOCK_CREATED"));
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
  void agentWorkflowAndAnalyticsShouldAcceptDemoRequests() throws Exception {
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
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.steps[0].node").value("intent"));

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
