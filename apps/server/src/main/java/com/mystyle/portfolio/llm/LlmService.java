package com.mystyle.portfolio.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphEdge;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphView;
import com.mystyle.portfolio.content.ContentModels.ModuleDemo;
import com.mystyle.portfolio.content.ContentModels.Project;
import com.mystyle.portfolio.jd.JdAnalysisResponse;
import com.mystyle.portfolio.jd.JdAnalysisResponse.ModuleRecommendation;
import com.mystyle.portfolio.jd.JdAnalysisResponse.ProjectRecommendation;
import com.mystyle.portfolio.resume.ResumeBasicInfoRequest;
import com.mystyle.portfolio.resume.ResumeModels.ResumeParsedPayload;
import com.mystyle.portfolio.resume.ResumeSectionItemRequest;
import com.mystyle.portfolio.resume.ResumeSectionType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class LlmService {
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final String apiKey;
  private final String baseUrl;
  private final String textModel;
  private final String visionModel;
  private volatile String authFailureMessage = "";

  public LlmService(
      ObjectMapper objectMapper,
      @Value("${app.llm.minimax.api-key:${MINIMAX_API_KEY:}}") String apiKey,
      @Value("${app.llm.minimax.base-url:${MINIMAX_BASE_URL:https://api.minimaxi.com/v1}}") String baseUrl,
      @Value("${app.llm.minimax.text-model:${MINIMAX_TEXT_MODEL:MiniMax-M2.7}}") String textModel,
      @Value("${app.llm.minimax.vision-model:${MINIMAX_VISION_MODEL:MiniMax-Text-01}}") String visionModel) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.baseUrl = trimTrailingSlash(baseUrl == null ? "https://api.minimaxi.com/v1" : baseUrl.trim());
    this.textModel = textModel == null || textModel.isBlank() ? "MiniMax-M2.7" : textModel.trim();
    this.visionModel = visionModel == null || visionModel.isBlank() ? "MiniMax-Text-01" : visionModel.trim();
    this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
  }

  public LlmProviderStatus status() {
    String mode = !configured() ? "missing-api-key" : authFailureMessage.isBlank() ? "minimax" : "auth-error";
    return new LlmProviderStatus(providerName(), configured(), textModel, visionModel, mode);
  }

  public List<KnowledgeRelationSuggestion> suggestKnowledgeRelations(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph) {
    return suggestKnowledgeRelationsDetailed(node, graph).suggestions();
  }

  public KnowledgeRelationResult suggestKnowledgeRelationsDetailed(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph) {
    return suggestKnowledgeRelationsDetailed(node, graph, RelationTask.FULL);
  }

  public KnowledgeRelationResult suggestKnowledgeParentRelationsDetailed(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph) {
    return suggestKnowledgeRelationsDetailed(node, graph, RelationTask.TERTIARY_PARENT_ONLY);
  }

  private KnowledgeRelationResult suggestKnowledgeRelationsDetailed(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph,
      RelationTask task) {
    String content = callMinimax(task == RelationTask.TERTIARY_PARENT_ONLY
        ? buildKnowledgeParentPrompt(node, graph)
        : buildKnowledgePrompt(node, graph));
    List<KnowledgeRelationSuggestion> suggestions = parseRelationSuggestions(content);
    List<KnowledgeRelationSuggestion> sanitized = sanitizeSuggestions(node, graph, suggestions);
    if (sanitized.isEmpty()) {
      throw ApiException.upstream("Minimax 已调用，但没有返回符合图谱层级规则的候选关系；本次不创建任何替代关系。");
    }
    return new KnowledgeRelationResult(
        providerName(),
        true,
        true,
        true,
        sanitized,
        List.of("Minimax 模型调用成功，已按图谱能力边界过滤候选关系。"));
  }

  public ResumeOptimizeResponse optimizeResume(ResumeOptimizeRequest request) {
    String role = request.targetRole() == null || request.targetRole().isBlank() ? "Java 后端开发" : request.targetRole().trim();
    List<String> hints = request.assetHints() == null ? List.of() : request.assetHints();
    if (Boolean.TRUE.equals(request.allowFallback())) {
      return new ResumeOptimizeResponse(
          "explicit-rule-fallback",
          role,
          "用户已确认使用规则降级，未调用 Minimax。",
          fallbackSummary(request.resumeText(), role),
          List.of(
              "优先保留真实项目证据和量化指标。",
              "围绕岗位 JD 重排项目顺序，突出匹配技术栈。",
              hints.isEmpty() ? "补充可验证的实习、项目和模块 Demo 证据。" : "可重点引用：" + String.join("、", hints)),
          List.of("项目经历：按 JD 关键词重排，先讲最匹配项目。", "技术能力：把 JD 高频技术栈前置。"),
          List.of("这是用户显式触发的规则降级结果，不是模型输出。"));
    }
    String content = callMinimax("""
        你是简历优化助手。只基于用户提供的真实信息给出中文建议，不编造经历。
        只能输出一个严格 JSON 对象，不要 Markdown，不要代码块，不要解释文字，不要 <think>。
        所有数组项必须是字符串，字符串内的双引号必须转义。
        返回 JSON：{"summary":"...","rewrittenSummary":"...","highlights":["..."],"sectionSuggestions":["..."],"riskNotes":["..."]}。
        目标岗位：%s
        简历文本：%s
        岗位JD：%s
        资产提示：%s
        """.formatted(role, nullToEmpty(request.resumeText()), nullToEmpty(request.jdText()), String.join("、", hints)));
    JsonNode json = extractJson(content);
    return new ResumeOptimizeResponse(
        providerName(),
        role,
        requiredText(json, "summary", "简历优化 summary"),
        requiredText(json, "rewrittenSummary", "简历优化 rewrittenSummary"),
        strings(json, "highlights"),
        strings(json, "sectionSuggestions"),
        strings(json, "riskNotes"));
  }

  public String runAgentWorkflow(String question, String templateId) {
    String content = callMinimax("""
        你是作品集里的 Agent Workflow 编排助手。请基于用户问题和模板生成一段真实模型回答。
        要求：
        1. 明确说明这是一次受约束的工作流回答，只能基于输入问题进行解释。
        2. 不要声称已经执行真实数据库查询、真实 RAG 检索或真实外部工具。
        3. 如果需要工具结果才能回答，明确说明缺少工具结果。
        4. 只能输出中文纯文本，120-240 字；不要 JSON，不要 Markdown，不要代码块，不要 <think>。
        用户问题：%s
        工作流模板：%s
        """.formatted(nullToEmpty(question), nullToEmpty(templateId)));
    if (content.isBlank()) {
      throw ApiException.upstream("Minimax Agent Workflow 返回空内容。");
    }
    return plainAgentAnswer(content);
  }

  public ResumeParsedPayload parseResumeText(String rawText) {
    String content = callMinimax("""
        你是中文简历结构化解析器。只抽取简历原文中存在的信息，不编造经历；原文没有的信息必须返回空字符串或空数组。
        只能输出一个严格 JSON 对象，不要 Markdown，不要代码块，不要解释文字，不要 <think>。
        basicInfo.name 必须从原文姓名抽取；basicInfo.education 必须从原文学校/学历抽取。
        basicInfo.title 和 basicInfo.summary 可以基于原文技能、项目、求职方向做一句真实归纳，但不得新增原文外经历。
        返回严格 JSON：
        {
          "basicInfo":{"name":"","title":"","summary":"","email":"","phone":"","location":"","education":"","githubUrl":"","websiteUrl":""},
          "sections":{
            "SKILL":[{"title":"","subtitle":"","period":"","summary":"","detail":"","tags":["Java"],"visible":true,"sortOrder":1}],
            "AWARD":[],
            "INTERNSHIP":[],
            "PROJECT":[],
            "ADVANTAGE":[]
          }
        }
        section 只允许 SKILL, AWARD, INTERNSHIP, PROJECT, ADVANTAGE。
        每个条目 detail 保留关键 bullet，summary 用一句话概括。
        简历原文：
        %s
        """.formatted(nullToEmpty(rawText)));
    JsonNode json = extractJson(content);
    ResumeBasicInfoRequest basicInfo = basicInfoFromJson(json.path("basicInfo"), rawText);
    Map<ResumeSectionType, List<ResumeSectionItemRequest>> sections = new EnumMap<>(ResumeSectionType.class);
    JsonNode sectionRoot = json.path("sections");
    for (ResumeSectionType type : ResumeSectionType.values()) {
      sections.put(type, sectionItemsFromJson(type, sectionRoot.path(type.name())));
    }
    if (sections.values().stream().allMatch(List::isEmpty)) {
      throw ApiException.upstream("Minimax 已调用，但没有返回任何可写入的简历板块；本次不生成替代草稿。");
    }
    return new ResumeParsedPayload(basicInfo, sections);
  }

  public JdAnalysisResponse analyzeJd(
      long analysisId,
      String jdText,
      String role,
      Set<String> keywords,
      int matchScore,
      List<Project> projects,
      List<ModuleDemo> modules) {
    String content = callMinimax("""
        你是 Java 后端秋招作品集 JD 适配助手。只能基于已有资产生成建议，不编造项目。
        只能输出一个严格 JSON 对象，不要 Markdown，不要代码块，不要解释文字，不要 <think>。
        projectRecommendations 和 moduleRecommendations 必须只使用输入资产中存在的 slug。
        返回 JSON：
        {
          "role":"岗位定位",
          "keywords":["Java"],
          "matchScore":88,
          "summary":"整体适配说明",
          "projectRecommendations":[{"slug":"已有项目slug","emphasis":"怎么讲","supportedBy":["证据"]}],
          "moduleRecommendations":[{"slug":"已有模块slug","reason":"为什么推荐"}],
          "resumeOptimizations":["简历优化建议"],
          "interviewTalkingPoints":["面试讲解点"],
          "riskNotes":["风险"]
        }
        projectRecommendations.slug 只能从项目资产里选择；moduleRecommendations.slug 只能从模块资产里选择。
        JD：%s
        初始岗位：%s
        初始关键词：%s
        项目资产：%s
        模块资产：%s
        """.formatted(nullToEmpty(jdText), role, String.join("、", keywords), toJson(projects), toJson(modules)));
    JsonNode json = extractJson(content);
    Set<String> projectSlugs = new LinkedHashSet<>(projects.stream().map(Project::slug).toList());
    Set<String> moduleSlugs = new LinkedHashSet<>(modules.stream().map(ModuleDemo::slug).toList());
    List<ProjectRecommendation> projectRecommendations = projectRecommendations(json.path("projectRecommendations"), projects, projectSlugs);
    List<ModuleRecommendation> moduleRecommendations = moduleRecommendations(json.path("moduleRecommendations"), modules, moduleSlugs);
    if (projectRecommendations.isEmpty() && moduleRecommendations.isEmpty()) {
      throw ApiException.upstream("Minimax 已调用，但没有返回任何合法的项目或模块推荐；本次不生成替代结果。");
    }
    return new JdAnalysisResponse(
        analysisId,
        providerName(),
        requiredText(json, "role", "JD 分析 role"),
        requiredStrings(json, "keywords", "JD 分析 keywords"),
        clampInt(requiredInt(json, "matchScore", "JD 分析 matchScore"), 0, 100),
        requiredText(json, "summary", "JD 分析 summary"),
        projectRecommendations,
        moduleRecommendations,
        requiredStrings(json, "resumeOptimizations", "JD 分析 resumeOptimizations"),
        requiredStrings(json, "interviewTalkingPoints", "JD 分析 interviewTalkingPoints"),
        strings(json, "riskNotes"));
  }

  public String providerName() {
    return "minimax-chat-completions";
  }

  public boolean configured() {
    return !apiKey.isBlank();
  }

  private String callMinimax(String prompt) {
    ensureConfigured();
    Map<String, Object> body = Map.of(
        "model", textModel,
        "messages", List.of(
            Map.of("role", "system", "content", "你是作品集知识图谱与简历优化助手。除非用户明确要求自然语言回答，否则输出必须是严格 JSON；不要输出 Markdown 代码块或 <think> 推理文本。"),
            Map.of("role", "user", "content", prompt)),
        "temperature", 0.2,
        "max_tokens", 3000);
    String response;
    try {
      response = restClient.post()
          .uri("/chat/completions")
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + apiKey)
          .body(body)
          .retrieve()
          .body(String.class);
    } catch (RestClientResponseException exception) {
      throw ApiException.upstream("Minimax HTTP 调用失败：" + exception.getStatusCode() + " " + trimTo(exception.getResponseBodyAsString(), 180));
    } catch (RuntimeException exception) {
      throw ApiException.upstream("Minimax 网络或客户端调用失败：" + trimTo(exception.getMessage(), 180));
    }
    JsonNode root;
    try {
      root = objectMapper.readTree(response);
    } catch (Exception exception) {
      throw ApiException.upstream("Minimax 响应不是合法 JSON：" + trimTo(response, 180));
    }
    JsonNode baseResp = root.path("base_resp");
    if (baseResp.path("status_code").asInt(0) != 0) {
      int statusCode = baseResp.path("status_code").asInt(0);
      String statusMessage = baseResp.path("status_msg").asText("unknown");
      String message = "Minimax API error: " + statusMessage;
      if (statusCode == 2049 || statusMessage.toLowerCase(Locale.ROOT).contains("invalid api key")) {
        authFailureMessage = message + "。请在 MiniMax Open Platform 的 Account Management > API Keys 重新生成有效 API Key。";
      }
      throw ApiException.upstream(message);
    }
    JsonNode content = root.path("choices").path(0).path("message").path("content");
    if (!content.isTextual() || content.asText().isBlank()) {
      throw ApiException.upstream("Minimax 响应缺少 choices[0].message.content。");
    }
    return stripReasoning(content.asText());
  }

  private String buildKnowledgePrompt(KnowledgeGraphNode node, KnowledgeGraphView graph) {
    List<Map<String, Object>> existingNodes = graph.nodes().stream()
        .map(item -> Map.<String, Object>of(
            "nodeKey", item.nodeKey(),
            "label", item.label(),
            "nodeType", item.nodeType(),
            "level", item.level(),
            "summary", item.summary(),
            "tags", item.tags()))
        .toList();
    return """
        请为当前知识图谱节点生成 1-3 条“必要关系”，只能连接到 existingNodes 中存在的节点。
        能力边界：
        1. 只表达作品集导航、项目证据、技术能力和博客解释之间的真实关系，不要为了图谱好看而造边。
        2. 每条关系必须直接包含当前节点，不要替其他两个节点新建关系。
        3. 层级契约：level 0 是一级个人核心，只能 CORE->SECTION；level 1 是二级栏目，只能 SECTION->level2；level 2 是三级内容节点，只能和所属二级栏目连接，或与其他 level2 节点建立证据关系。
        4. 禁止跨级连接，例如 level2 不能直接连 level0；禁止反向指向 CORE；禁止 SECTION 之间互连。
        5. 优先生成 1 条归属边：CORE->SECTION 用 OWNS；section-blog->BLOG 用 CONTAINS；section-evidence->PROJECT/MODULE 用 INCLUDES；section-resume->SKILL 用 INCLUDES。
        6. 证据边最多 2 条：PROJECT/MODULE->SKILL 用 USES；BLOG->SKILL/PROJECT/MODULE 用 EXPLAINS；只有标签和摘要明确重合时才用 RELATED。
        关系类型只能使用 OWNS, INCLUDES, CONTAINS, USES, EXPLAINS, RELATED。
        输出契约：返回严格 JSON 数组，不要 Markdown，不要解释文字，不要 <think>。
        JSON schema: [{"fromNodeKey":"existing node key","toNodeKey":"existing node key","relationType":"OWNS|INCLUDES|CONTAINS|USES|EXPLAINS|RELATED","reason":"不超过60字的依据"}]。
        新节点：%s
        已有节点：%s
        """.formatted(toJson(node), toJson(existingNodes));
  }

  private String buildKnowledgeParentPrompt(KnowledgeGraphNode node, KnowledgeGraphView graph) {
    List<Map<String, Object>> sectionNodes = graph.nodes().stream()
        .filter(item -> item.level() == 1)
        .map(item -> Map.<String, Object>of(
            "nodeKey", item.nodeKey(),
            "label", item.label(),
            "nodeType", item.nodeType(),
            "level", item.level(),
            "summary", item.summary(),
            "tags", item.tags()))
        .toList();
    return """
        请只为当前三级知识图谱节点选择最合理的二级栏目归属边。
        这是全图编排任务，一级个人节点与二级栏目之间的关系已经确定，不要输出 CORE、me、OWNS，也不要输出三级节点之间的证据边。
        输入输出契约：
        1. 当前节点必须是 level 2；只能从 sectionNodes 中选择 level 1 栏目作为 fromNodeKey。
        2. 输出方向必须是 二级栏目 -> 当前三级节点，即 fromNodeKey 为 sectionNodes 的 nodeKey，toNodeKey 为 currentNode.nodeKey。
        3. BLOG 归属技术博客时用 CONTAINS；SKILL/PROJECT/MODULE 归属履历、项目证据、面试助手等栏目时用 INCLUDES。
        4. 最多返回 1-2 条；没有明确归属时返回空数组，不要为了图谱好看造边。
        5. 禁止跨级连接，禁止输出 level2 -> level0、level0 -> level1、level2 -> level2。
        输出契约：返回严格 JSON 数组，不要 Markdown，不要解释文字，不要 <think>。
        JSON schema: [{"fromNodeKey":"section node key","toNodeKey":"current node key","relationType":"INCLUDES|CONTAINS","reason":"不超过60字的依据"}]。
        currentNode: %s
        sectionNodes: %s
        """.formatted(toJson(node), toJson(sectionNodes));
  }

  private List<KnowledgeRelationSuggestion> parseRelationSuggestions(String content) {
    try {
      JsonNode json = extractJson(content);
      JsonNode array = json.isArray() ? json : json.path("relations");
      if (!array.isArray()) {
        throw ApiException.upstream("Minimax 图谱关系响应不是 JSON 数组。");
      }
      return objectMapper.convertValue(array, new TypeReference<List<KnowledgeRelationSuggestion>>() {});
    } catch (RuntimeException exception) {
      if (exception instanceof ApiException apiException) {
        throw apiException;
      }
      throw ApiException.upstream("Minimax 图谱关系响应解析失败：" + trimTo(exception.getMessage(), 180));
    }
  }

  private List<KnowledgeRelationSuggestion> sanitizeSuggestions(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph,
      List<KnowledgeRelationSuggestion> suggestions) {
    Set<String> keys = new LinkedHashSet<>(graph.nodes().stream().map(KnowledgeGraphNode::nodeKey).toList());
    keys.add(node.nodeKey());
    return suggestions.stream()
        .filter(item -> keys.contains(item.fromNodeKey()) && keys.contains(item.toNodeKey()))
        .filter(item -> !item.fromNodeKey().equals(item.toNodeKey()))
        .map(item -> new KnowledgeRelationSuggestion(
            item.fromNodeKey(),
            item.toNodeKey(),
            allowedRelation(item.relationType()),
            nullToEmpty(item.reason())))
        .filter(item -> touchesNode(item, node.nodeKey()))
        .limit(3)
        .toList();
  }

  private JsonNode extractJson(String content) {
    try {
      return parseJsonContent(content);
    } catch (ApiException parseException) {
      String repaired = callMinimax("""
          下面是一段模型输出，目标是把它转换成严格 JSON。
          只输出 JSON，不要 Markdown，不要解释，不要 <think>。
          保留原有事实，不补造新信息；无法确定的字段用空字符串或空数组。
          原始输出：
          %s
          """.formatted(nullToEmpty(content)));
      try {
        return parseJsonContent(repaired);
      } catch (ApiException repairException) {
        throw ApiException.upstream("模型返回无法解析为 JSON，自动 JSON 修复也失败：" + trimTo(repairException.getMessage(), 160));
      }
    }
  }

  private JsonNode parseJsonContent(String content) {
    try {
      String normalized = stripReasoning(content == null ? "" : content.trim());
      normalized = normalized
          .replaceFirst("(?is)^```json\\s*", "")
          .replaceFirst("(?is)^```\\s*", "")
          .replaceFirst("(?is)```\\s*$", "")
          .trim();
      int startArray = normalized.indexOf('[');
      int startObject = normalized.indexOf('{');
      int start;
      if (startArray >= 0 && startObject >= 0) {
        start = Math.min(startArray, startObject);
      } else {
        start = Math.max(startArray, startObject);
      }
      int end = Math.max(normalized.lastIndexOf(']'), normalized.lastIndexOf('}'));
      if (start >= 0 && end > start) {
        normalized = normalized.substring(start, end + 1);
      }
      return objectMapper.readTree(normalized);
    } catch (Exception exception) {
      throw ApiException.upstream("模型返回无法解析为 JSON：" + trimTo(exception.getMessage(), 180));
    }
  }

  private List<String> strings(JsonNode json, String field) {
    JsonNode values = json.path(field);
    if (!values.isArray()) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    values.forEach(value -> {
      if (value.isTextual() && !value.asText().isBlank()) {
        result.add(value.asText());
      }
    });
    return result;
  }

  private String text(JsonNode json, String field, String defaultValue) {
    JsonNode value = json.path(field);
    return value.isTextual() && !value.asText().isBlank() ? value.asText() : defaultValue;
  }

  private String requiredText(JsonNode json, String field, String label) {
    JsonNode value = json.path(field);
    if (!value.isTextual() || value.asText().isBlank()) {
      throw ApiException.upstream("Minimax 响应缺少必需字段：" + label);
    }
    return value.asText().trim();
  }

  private List<String> requiredStrings(JsonNode json, String field, String label) {
    List<String> values = strings(json, field);
    if (values.isEmpty()) {
      throw ApiException.upstream("Minimax 响应缺少必需数组字段：" + label);
    }
    return values;
  }

  private int requiredInt(JsonNode json, String field, String label) {
    JsonNode value = json.path(field);
    if (!value.canConvertToInt()) {
      throw ApiException.upstream("Minimax 响应缺少必需数字字段：" + label);
    }
    return value.asInt();
  }

  private ResumeBasicInfoRequest basicInfoFromJson(JsonNode json, String rawText) {
    String name = text(json, "name", "");
    String title = text(json, "title", "");
    String summary = text(json, "summary", "");
    String education = text(json, "education", "");
    if (name.isBlank() || title.isBlank() || summary.isBlank() || education.isBlank()) {
      throw ApiException.upstream("Minimax 简历扫描缺少 name/title/summary/education，已拒绝生成占位信息。");
    }
    return new ResumeBasicInfoRequest(
        name,
        title,
        summary,
        text(json, "email", ""),
        text(json, "phone", ""),
        text(json, "location", ""),
        education,
        text(json, "githubUrl", ""),
        text(json, "websiteUrl", ""));
  }

  private List<ResumeSectionItemRequest> sectionItemsFromJson(ResumeSectionType type, JsonNode array) {
    if (!array.isArray()) {
      return List.of();
    }
    List<ResumeSectionItemRequest> items = new ArrayList<>();
    int nextOrder = 1;
    for (JsonNode item : array) {
      String title = text(item, "title", "");
      if (title.isBlank()) {
        continue;
      }
      items.add(new ResumeSectionItemRequest(
          trimTo(title, 160),
          trimTo(text(item, "subtitle", ""), 160),
          trimTo(text(item, "period", ""), 64),
          text(item, "summary", ""),
          text(item, "detail", text(item, "summary", "")),
          strings(item, "tags"),
          item.path("visible").isMissingNode() || item.path("visible").asBoolean(true),
          item.path("sortOrder").asInt(nextOrder)));
      nextOrder++;
    }
    return items;
  }

  private List<ProjectRecommendation> projectRecommendations(JsonNode array, List<Project> projects, Set<String> allowedSlugs) {
    if (!array.isArray()) return List.of();
    Map<String, Project> bySlug = projects.stream().collect(java.util.stream.Collectors.toMap(Project::slug, item -> item, (a, b) -> a));
    List<ProjectRecommendation> result = new ArrayList<>();
    for (JsonNode item : array) {
      String slug = text(item, "slug", "");
      if (!allowedSlugs.contains(slug)) continue;
      Project project = bySlug.get(slug);
      result.add(new ProjectRecommendation(
          slug,
          project == null ? slug : project.name(),
          requiredText(item, "emphasis", "projectRecommendations.emphasis"),
          strings(item, "supportedBy")));
    }
    return result;
  }

  private List<ModuleRecommendation> moduleRecommendations(JsonNode array, List<ModuleDemo> modules, Set<String> allowedSlugs) {
    if (!array.isArray()) return List.of();
    Map<String, ModuleDemo> bySlug = modules.stream().collect(java.util.stream.Collectors.toMap(ModuleDemo::slug, item -> item, (a, b) -> a));
    List<ModuleRecommendation> result = new ArrayList<>();
    for (JsonNode item : array) {
      String slug = text(item, "slug", "");
      if (!allowedSlugs.contains(slug)) continue;
      ModuleDemo module = bySlug.get(slug);
      result.add(new ModuleRecommendation(
          slug,
          module == null ? slug : module.title(),
          requiredText(item, "reason", "moduleRecommendations.reason")));
    }
    return result;
  }

  private int clampInt(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private String fallbackSummary(String resumeText, String role) {
    String prefix = "面向" + role + "，建议突出真实工程证据、技术栈匹配和可复盘结果。";
    if (resumeText != null && resumeText.contains("Redis")) {
      return prefix + " 可优先强调 Redis、MySQL、Spring Boot 等后端能力。";
    }
    return prefix;
  }

  private String trimTo(String value, int maxLength) {
    String normalized = value == null ? "" : value.trim();
    return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
  }

  private void ensureConfigured() {
    if (!configured()) {
      throw ApiException.serviceUnavailable("未配置 MINIMAX_API_KEY，AI 接口不会生成替代结果或返回假数据。");
    }
    if (!authFailureMessage.isBlank()) {
      throw ApiException.serviceUnavailable("Minimax API Key 当前不可用，AI 接口不会生成替代结果或返回假数据：" + authFailureMessage);
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception exception) {
      return String.valueOf(value);
    }
  }

  private String allowedRelation(String relationType) {
    String normalized = relationType == null ? "RELATED" : relationType.trim().toUpperCase(Locale.ROOT);
    return Set.of("OWNS", "INCLUDES", "CONTAINS", "USES", "EXPLAINS", "RELATED").contains(normalized)
        ? normalized
        : "RELATED";
  }

  private boolean touchesNode(KnowledgeRelationSuggestion suggestion, String nodeKey) {
    return nodeKey.equals(suggestion.fromNodeKey()) || nodeKey.equals(suggestion.toNodeKey());
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String plainAgentAnswer(String content) {
    String normalized = content == null ? "" : content.trim();
    try {
      JsonNode json = parseJsonContent(normalized);
      String response = text(json, "response", "");
      if (!response.isBlank()) {
        return response;
      }
      String answer = text(json, "answer", "");
      if (!answer.isBlank()) {
        return answer;
      }
    } catch (ApiException ignored) {
      // Natural-language agent answers are allowed; JSON parsing is only a cleanup path.
    }
    return normalized;
  }

  private String stripReasoning(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    String stripped = value.replaceAll("(?is)<think>.*?</think>", "").trim();
    return stripped.isBlank() ? value.trim() : stripped;
  }

  private String trimTrailingSlash(String value) {
    while (value.endsWith("/")) {
      value = value.substring(0, value.length() - 1);
    }
    return value;
  }

  private enum RelationTask {
    FULL,
    TERTIARY_PARENT_ONLY
  }
}
