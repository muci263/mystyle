package com.mystyle.portfolio.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphEdge;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphView;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LlmService {
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final String apiKey;
  private final String baseUrl;
  private final String textModel;
  private final String visionModel;

  public LlmService(
      ObjectMapper objectMapper,
      @Value("${app.llm.minimax.api-key:${MINIMAX_API_KEY:}}") String apiKey,
      @Value("${app.llm.minimax.base-url:${MINIMAX_BASE_URL:https://api.minimax.io/v1}}") String baseUrl,
      @Value("${app.llm.minimax.text-model:${MINIMAX_TEXT_MODEL:MiniMax-M1}}") String textModel,
      @Value("${app.llm.minimax.vision-model:${MINIMAX_VISION_MODEL:MiniMax-Text-01}}") String visionModel) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.baseUrl = trimTrailingSlash(baseUrl == null ? "https://api.minimax.io/v1" : baseUrl.trim());
    this.textModel = textModel == null || textModel.isBlank() ? "MiniMax-M1" : textModel.trim();
    this.visionModel = visionModel == null || visionModel.isBlank() ? "MiniMax-Text-01" : visionModel.trim();
    this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
  }

  public LlmProviderStatus status() {
    return new LlmProviderStatus(providerName(), configured(), textModel, visionModel, configured() ? "minimax" : "fallback");
  }

  public List<KnowledgeRelationSuggestion> suggestKnowledgeRelations(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph) {
    List<KnowledgeRelationSuggestion> fallback = fallbackRelations(node, graph);
    if (!configured()) {
      return fallback;
    }
    try {
      String content = callMinimax(buildKnowledgePrompt(node, graph));
      List<KnowledgeRelationSuggestion> suggestions = parseRelationSuggestions(content);
      return suggestions.isEmpty() ? fallback : sanitizeSuggestions(node, graph, suggestions);
    } catch (RuntimeException exception) {
      return fallback;
    }
  }

  public ResumeOptimizeResponse optimizeResume(ResumeOptimizeRequest request) {
    String role = request.targetRole() == null || request.targetRole().isBlank() ? "Java 后端开发" : request.targetRole().trim();
    List<String> hints = request.assetHints() == null ? List.of() : request.assetHints();
    if (!configured()) {
      return new ResumeOptimizeResponse(
          providerName(),
          role,
          "当前未配置 Minimax API Key，已使用规则型 fallback 生成优化建议。",
          List.of(
              "优先保留真实项目证据和量化指标。",
              "围绕岗位 JD 重排项目顺序，突出匹配技术栈。",
              hints.isEmpty() ? "补充可验证的实习、项目和模块 Demo 证据。" : "可重点引用：" + String.join("、", hints)),
          List.of("fallback 未调用真实模型；提交前仍需人工校验经历真实性。"));
    }
    try {
      String content = callMinimax("""
          你是简历优化助手。只基于用户提供的真实信息给出中文建议，不编造经历。
          返回 JSON：{"summary":"...","highlights":["..."],"riskNotes":["..."]}。
          目标岗位：%s
          简历文本：%s
          岗位JD：%s
          资产提示：%s
          """.formatted(role, nullToEmpty(request.resumeText()), nullToEmpty(request.jdText()), String.join("、", hints)));
      JsonNode json = extractJson(content);
      return new ResumeOptimizeResponse(
          providerName(),
          role,
          text(json, "summary", "已基于岗位要求生成优化建议。"),
          strings(json, "highlights"),
          strings(json, "riskNotes"));
    } catch (RuntimeException exception) {
      return new ResumeOptimizeResponse(
          providerName(),
          role,
          "Minimax 调用失败，已降级为规则型建议。",
          List.of("突出 JD 中反复出现的技术关键词。", "每段项目经历都补充问题、方案、结果。"),
          List.of("请检查 MINIMAX_API_KEY、网络和模型名称。"));
    }
  }

  public String providerName() {
    return configured() ? "minimax-chat-completions" : "mock-rule-provider";
  }

  public boolean configured() {
    return !apiKey.isBlank();
  }

  private String callMinimax(String prompt) {
    Map<String, Object> body = Map.of(
        "model", textModel,
        "messages", List.of(
            Map.of("role", "system", "content", "你是作品集知识图谱与简历优化助手，输出必须可被程序解析。"),
            Map.of("role", "user", "content", prompt)),
        "temperature", 0.2,
        "max_tokens", 1200);
    String response = restClient.post()
        .uri("/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + apiKey)
        .body(body)
        .retrieve()
        .body(String.class);
    try {
      JsonNode root = objectMapper.readTree(response);
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      return content.isTextual() ? content.asText() : response;
    } catch (Exception exception) {
      return response == null ? "" : response;
    }
  }

  private String buildKnowledgePrompt(KnowledgeGraphNode node, KnowledgeGraphView graph) {
    List<Map<String, Object>> existingNodes = graph.nodes().stream()
        .map(item -> Map.<String, Object>of(
            "nodeKey", item.nodeKey(),
            "label", item.label(),
            "nodeType", item.nodeType(),
            "summary", item.summary(),
            "tags", item.tags()))
        .toList();
    return """
        请为新增/更新的知识图谱节点生成 1-5 条关系，只能连接到 existingNodes 中存在的节点。
        关系类型只能使用 OWNS, INCLUDES, CONTAINS, USES, EXPLAINS, RELATED。
        返回 JSON 数组：[{"fromNodeKey":"...","toNodeKey":"...","relationType":"...","reason":"..."}]。
        新节点：%s
        已有节点：%s
        """.formatted(toJson(node), toJson(existingNodes));
  }

  private List<KnowledgeRelationSuggestion> parseRelationSuggestions(String content) {
    try {
      JsonNode json = extractJson(content);
      JsonNode array = json.isArray() ? json : json.path("relations");
      if (!array.isArray()) {
        return List.of();
      }
      return objectMapper.convertValue(array, new TypeReference<List<KnowledgeRelationSuggestion>>() {});
    } catch (RuntimeException exception) {
      return List.of();
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
        .limit(5)
        .toList();
  }

  private List<KnowledgeRelationSuggestion> fallbackRelations(KnowledgeGraphNode node, KnowledgeGraphView graph) {
    List<KnowledgeRelationSuggestion> suggestions = new ArrayList<>();
    String type = node.nodeType().toUpperCase(Locale.ROOT);
    if (!"CORE".equals(type) && graph.nodes().stream().anyMatch(item -> item.nodeKey().equals("me"))) {
      if ("SECTION".equals(type)) {
        suggestions.add(new KnowledgeRelationSuggestion("me", node.nodeKey(), "OWNS", "一级栏目归属于个人核心。"));
      } else if ("BLOG".equals(type)) {
        suggestions.add(new KnowledgeRelationSuggestion("section-blog", node.nodeKey(), "CONTAINS", "博客节点归入技术博客栏目。"));
      } else if ("PROJECT".equals(type) || "MODULE".equals(type)) {
        suggestions.add(new KnowledgeRelationSuggestion("section-evidence", node.nodeKey(), "INCLUDES", "项目与模块归入项目证据。"));
      } else {
        suggestions.add(new KnowledgeRelationSuggestion("section-resume", node.nodeKey(), "INCLUDES", "能力节点归入履历能力集合。"));
      }
    }

    Set<String> nodeTags = lowerSet(node.tags());
    graph.nodes().stream()
        .filter(item -> !item.nodeKey().equals(node.nodeKey()))
        .filter(item -> !item.nodeKey().startsWith("section-"))
        .filter(item -> intersects(nodeTags, lowerSet(item.tags())) || containsAny(node.summary() + " " + node.content(), item.tags()))
        .limit(4)
        .forEach(item -> suggestions.add(new KnowledgeRelationSuggestion(
            node.nodeKey(),
            item.nodeKey(),
            relationFor(node.nodeType(), item.nodeType()),
            "基于标签或摘要关键词匹配。")));
    return suggestions.stream().limit(5).toList();
  }

  private JsonNode extractJson(String content) {
    try {
      String normalized = content == null ? "" : content.trim();
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
      throw new IllegalArgumentException("模型返回无法解析为 JSON", exception);
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

  private String text(JsonNode json, String field, String fallback) {
    JsonNode value = json.path(field);
    return value.isTextual() && !value.asText().isBlank() ? value.asText() : fallback;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception exception) {
      return String.valueOf(value);
    }
  }

  private String relationFor(String fromType, String toType) {
    String from = fromType.toUpperCase(Locale.ROOT);
    String to = toType.toUpperCase(Locale.ROOT);
    if ("BLOG".equals(from) && "SKILL".equals(to)) return "EXPLAINS";
    if ("PROJECT".equals(from) || "MODULE".equals(from)) return "USES";
    return "RELATED";
  }

  private String allowedRelation(String relationType) {
    String normalized = relationType == null ? "RELATED" : relationType.trim().toUpperCase(Locale.ROOT);
    return Set.of("OWNS", "INCLUDES", "CONTAINS", "USES", "EXPLAINS", "RELATED").contains(normalized)
        ? normalized
        : "RELATED";
  }

  private Set<String> lowerSet(List<String> values) {
    if (values == null) return Set.of();
    Set<String> result = new LinkedHashSet<>();
    values.forEach(value -> {
      if (value != null && !value.isBlank()) {
        result.add(value.toLowerCase(Locale.ROOT));
      }
    });
    return result;
  }

  private boolean intersects(Set<String> left, Set<String> right) {
    return left.stream().anyMatch(right::contains);
  }

  private boolean containsAny(String text, List<String> values) {
    String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
    return values != null && values.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .anyMatch(normalized::contains);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String trimTrailingSlash(String value) {
    while (value.endsWith("/")) {
      value = value.substring(0, value.length() - 1);
    }
    return value;
  }
}
