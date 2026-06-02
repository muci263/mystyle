package com.mystyle.portfolio.jd;

import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.content.ContentModels.ModuleDemo;
import com.mystyle.portfolio.content.ContentModels.Project;
import com.mystyle.portfolio.content.PortfolioContentService;
import com.mystyle.portfolio.jd.JdAnalysisResponse.ModuleRecommendation;
import com.mystyle.portfolio.jd.JdAnalysisResponse.ProjectRecommendation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class JdAnalysisService {
  private static final List<String> WATCHED_KEYWORDS = List.of(
      "Java", "Spring Boot", "Spring Cloud", "Redis", "MySQL", "微服务", "Docker", "Nginx",
      "Jenkins", "RAG", "Agent", "LLM", "SQL", "AI", "性能优化");

  private final AtomicLong idGenerator = new AtomicLong(1);
  private final Map<Long, JdAnalysisResponse> analyses = new ConcurrentHashMap<>();
  private final PortfolioContentService contentService;

  public JdAnalysisService(PortfolioContentService contentService) {
    this.contentService = contentService;
  }

  public JdAnalysisResponse analyze(JdAnalyzeRequest request) {
    String jdText = request.jd();
    Set<String> keywords = extractKeywords(jdText);
    if (request.target() != null) {
      keywords.addAll(request.target());
    }

    List<Project> orderedProjects = orderProjects(keywords);
    List<ModuleDemo> orderedModules = orderModules(keywords);
    int matchScore = calculateScore(keywords, orderedProjects, orderedModules);
    String role = inferRole(jdText, keywords);
    long analysisId = idGenerator.getAndIncrement();

    JdAnalysisResponse response = new JdAnalysisResponse(
        analysisId,
        "mock-rule-provider",
        role,
        List.copyOf(keywords),
        matchScore,
        "基于当前经历资产，建议优先展示与岗位关键词直接匹配的项目、模块 Demo 和量化结果。",
        orderedProjects.stream()
            .map(project -> new ProjectRecommendation(
                project.slug(),
                project.name(),
                buildProjectEmphasis(project, keywords),
                project.metrics()))
            .toList(),
        orderedModules.stream()
            .map(module -> new ModuleRecommendation(
                module.slug(),
                module.title(),
                buildModuleReason(module, keywords)))
            .toList(),
        List.of(
            "当前为规则型 mock Provider，未调用真实 LLM。",
            "生成内容只基于已有经历资产，不能新增未验证经历。",
            "后续接入真实 LLM 后需要保留来源支撑和人工确认。"));
    analyses.put(analysisId, response);
    return response;
  }

  public ProfileVariantResponse createVariant(long analysisId) {
    JdAnalysisResponse analysis = analyses.get(analysisId);
    if (analysis == null) {
      throw ApiException.notFound("JD 分析记录不存在");
    }
    return new ProfileVariantResponse(
        analysisId + 10_000,
        analysisId,
        analysis.role() + " 定制展示",
        "我是赵豪然，主攻 Java 后端开发，重点展示 " + String.join("、", analysis.keywords()) + " 相关经历。",
        analysis.projectRecommendations().stream().map(ProjectRecommendation::slug).toList(),
        analysis.moduleRecommendations().stream().map(ModuleRecommendation::slug).toList(),
        "MOCK_CREATED");
  }

  private Set<String> extractKeywords(String jdText) {
    String normalized = jdText.toLowerCase(Locale.ROOT);
    Set<String> keywords = new LinkedHashSet<>();
    for (String keyword : WATCHED_KEYWORDS) {
      if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
        keywords.add(keyword);
      }
    }
    if (keywords.isEmpty()) {
      keywords.addAll(List.of("Java", "Spring Boot", "项目经历"));
    }
    return keywords;
  }

  private List<Project> orderProjects(Set<String> keywords) {
    return contentService.projects(null).stream()
        .sorted((left, right) -> Integer.compare(scoreProject(right, keywords), scoreProject(left, keywords)))
        .toList();
  }

  private List<ModuleDemo> orderModules(Set<String> keywords) {
    return contentService.moduleDemos(null).stream()
        .sorted((left, right) -> Integer.compare(scoreModule(right, keywords), scoreModule(left, keywords)))
        .toList();
  }

  private int calculateScore(Set<String> keywords, List<Project> projects, List<ModuleDemo> modules) {
    int score = 60 + Math.min(24, keywords.size() * 4);
    score += Math.min(8, scoreProject(projects.getFirst(), keywords));
    score += Math.min(8, scoreModule(modules.getFirst(), keywords));
    return Math.min(96, score);
  }

  private int scoreProject(Project project, Set<String> keywords) {
    return intersection(project.tech(), keywords).size() * 4 + intersection(project.metrics(), keywords).size() * 2;
  }

  private int scoreModule(ModuleDemo module, Set<String> keywords) {
    return intersection(module.tech(), keywords).size() * 4 + intersection(module.talkingPoints(), keywords).size() * 2;
  }

  private String inferRole(String jdText, Set<String> keywords) {
    String normalized = jdText.toLowerCase(Locale.ROOT);
    if (normalized.contains("ai") || normalized.contains("llm") || normalized.contains("rag") || keywords.contains("Agent")) {
      return "Java 后端 / AI 应用工程化";
    }
    if (normalized.contains("实习")) {
      return "Java 后端开发实习生";
    }
    return "Java 后端开发";
  }

  private String buildProjectEmphasis(Project project, Set<String> keywords) {
    List<String> matched = intersection(project.tech(), keywords);
    if (matched.isEmpty()) {
      return "重点讲清业务背景、我的职责、接口实现和可量化结果。";
    }
    return "重点突出 " + String.join("、", matched) + " 相关实现，以及 " + String.join("、", project.metrics()) + "。";
  }

  private String buildModuleReason(ModuleDemo module, Set<String> keywords) {
    List<String> matched = intersection(module.tech(), keywords);
    if (matched.isEmpty()) {
      return "该模块可用于展示 " + module.project() + " 中的工程化拆解能力。";
    }
    return "该模块覆盖 " + String.join(" / ", matched) + " 等岗位关注点。";
  }

  private List<String> intersection(List<String> source, Set<String> keywords) {
    List<String> matched = new ArrayList<>();
    for (String item : source) {
      String normalized = item.toLowerCase(Locale.ROOT);
      boolean hit = keywords.stream()
          .map(keyword -> keyword.toLowerCase(Locale.ROOT))
          .anyMatch(normalized::contains);
      if (hit) {
        matched.add(item);
      }
    }
    return matched;
  }
}
