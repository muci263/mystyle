package com.mystyle.portfolio.knowledge;

import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphEdge;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphView;
import com.mystyle.portfolio.content.PortfolioContentService;
import com.mystyle.portfolio.llm.KnowledgeRelationResult;
import com.mystyle.portfolio.llm.KnowledgeRelationSuggestion;
import com.mystyle.portfolio.llm.LlmService;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeGraphSmartService {
  private static final int MAX_EDGES_PER_NODE = 3;
  private static final int MAX_ORCHESTRATE_EVIDENCE_EDGES_PER_NODE = 3;
  private static final int MAX_ORCHESTRATE_CREATED_EDGES = 24;
  private static final int MAX_ORCHESTRATE_LLM_NODES = 32;
  private static final int MAX_ORCHESTRATE_PARALLELISM = 4;

  private final PortfolioContentService contentService;
  private final LlmService llmService;

  public KnowledgeGraphSmartService(PortfolioContentService contentService, LlmService llmService) {
    this.contentService = contentService;
    this.llmService = llmService;
  }

  public KnowledgeGraphAutoRelateResponse smartCreate(KnowledgeGraphSmartNodeRequest request) {
    return smartCreate(request, false);
  }

  public KnowledgeGraphAutoRelateResponse smartCreate(KnowledgeGraphSmartNodeRequest request, boolean allowFallback) {
    KnowledgeGraphNodeRequest nodeRequest = toNodeRequest(request);
    KnowledgeGraphNode createdNode = contentService.createKnowledgeGraphNode(nodeRequest);
    return autoRelate(createdNode.nodeKey(), allowFallback);
  }

  public KnowledgeGraphAutoRelateResponse autoRelate(String nodeKey) {
    return autoRelate(nodeKey, false);
  }

  public KnowledgeGraphAutoRelateResponse autoRelate(String nodeKey, boolean allowFallback) {
    KnowledgeGraphNode node = contentService.knowledgeGraphNodes(true).stream()
        .filter(item -> item.nodeKey().equals(nodeKey))
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("图谱节点不存在"));
    KnowledgeGraphView graph = graphIncludingHidden();
    RelationDraft draft = allowFallback
        ? fallbackDraft(node, graph, RelationScope.FULL)
        : proposeRelations(node, graph, RelationScope.FULL);
    return saveDraft(draft, MAX_EDGES_PER_NODE);
  }

  public KnowledgeGraphOrchestrateResponse orchestrate() {
    return orchestrate(false);
  }

  public KnowledgeGraphOrchestrateResponse orchestrate(boolean allowFallback) {
    KnowledgeGraphView graph = graphIncludingHidden();
    Map<String, Long> parentEdgeCountByNode = parentEdgeCountByTertiary(graph);
    List<KnowledgeGraphNode> candidates = graph.nodes().stream()
        .filter(node -> node.level() == 2)
        .filter(node -> KnowledgeGraphHierarchy.isTertiary(node.nodeType()))
        .sorted((left, right) -> {
          int edgeCountCompare = Long.compare(
              parentEdgeCountByNode.getOrDefault(left.nodeKey(), 0L),
              parentEdgeCountByNode.getOrDefault(right.nodeKey(), 0L));
          return edgeCountCompare != 0 ? edgeCountCompare : Integer.compare(left.sortOrder(), right.sortOrder());
        })
        .toList();
    List<KnowledgeGraphNode> nodes = candidates.stream()
        .limit(MAX_ORCHESTRATE_LLM_NODES)
        .toList();
    List<RelationDraft> drafts = allowFallback
        ? nodes.stream().map(node -> fallbackDraft(node, graph, RelationScope.TERTIARY_ORCHESTRATE)).toList()
        : proposeRelationsInParallel(graph, nodes);
    List<KnowledgeGraphAutoRelateResponse> results = new ArrayList<>();
    int createdCount = 0;
    for (RelationDraft draft : drafts) {
      if (createdCount >= MAX_ORCHESTRATE_CREATED_EDGES) {
        break;
      }
      KnowledgeGraphAutoRelateResponse response = saveDraft(draft, suggestionLimit(graph, draft.scope()));
      results.add(response);
      createdCount += response.createdEdges().size();
    }
    List<String> notes = new ArrayList<>();
    notes.add("已筛选 " + candidates.size() + " 个三级节点，本次扫描 " + nodes.size() + " 个。");
    notes.add("新增关系 " + createdCount + " 条。");
    notes.add("编排边界：处理二级栏目到三级内容的归属边，以及三级内容之间的证据边；不处理一级到二级固定关系。");
    notes.add("新增边默认隐藏；全局最多新增 " + MAX_ORCHESTRATE_CREATED_EDGES + " 条；单节点本次可补多个二级归属，并最多补 " + MAX_ORCHESTRATE_EVIDENCE_EDGES_PER_NODE + " 条三级证据关系。");
    if (candidates.size() > nodes.size()) {
      notes.add("还有 " + (candidates.size() - nodes.size()) + " 个三级节点可继续点击全图编排。");
    }
    if (nodes.isEmpty()) {
      notes.add("当前没有三级内容节点可编排。");
    }
    if (allowFallback) {
      notes.add("已生成本地候选关系，请人工确认后使用。");
    }
    return new KnowledgeGraphOrchestrateResponse(allowFallback ? "local-template" : llmService.providerName(), nodes.size(), createdCount, results, notes);
  }

  private List<RelationDraft> proposeRelationsInParallel(KnowledgeGraphView graph, List<KnowledgeGraphNode> nodes) {
    if (nodes.isEmpty()) {
      return List.of();
    }
    ExecutorService executor = Executors.newFixedThreadPool(Math.min(MAX_ORCHESTRATE_PARALLELISM, nodes.size()));
    try {
      List<CompletableFuture<RelationDraft>> futures = nodes.stream()
          .map(node -> CompletableFuture.supplyAsync(
              () -> proposeRelations(node, graph, RelationScope.TERTIARY_ORCHESTRATE),
              executor))
          .toList();
      try {
        return futures.stream().map(CompletableFuture::join).toList();
      } catch (CompletionException exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        if (cause instanceof ApiException apiException) {
          throw apiException;
        }
        throw ApiException.upstream("并行编排过程中 Minimax 调用失败：" + trimOrDefault(cause.getMessage(), cause.getClass().getSimpleName()));
      }
    } finally {
      executor.shutdown();
    }
  }

  private RelationDraft proposeRelations(KnowledgeGraphNode node, KnowledgeGraphView graph, RelationScope scope) {
    KnowledgeRelationResult relationResult = switch (scope) {
      case TERTIARY_PARENT_ONLY -> llmService.suggestKnowledgeParentRelationsDetailed(node, graph);
      case TERTIARY_ORCHESTRATE -> llmService.suggestKnowledgeOrchestrateRelationsDetailed(node, graph);
      case FULL -> llmService.suggestKnowledgeRelationsDetailed(node, graph);
    };
    List<KnowledgeRelationSuggestion> suggestions = boundedSuggestions(node, graph, relationResult.suggestions(), scope);
    return new RelationDraft(node, relationResult, suggestions, scope);
  }

  private RelationDraft fallbackDraft(KnowledgeGraphNode node, KnowledgeGraphView graph, RelationScope scope) {
    List<KnowledgeRelationSuggestion> suggestions = scope == RelationScope.TERTIARY_PARENT_ONLY
        ? fallbackParentSuggestions(node, graph)
        : fallbackSuggestions(node, graph);
    KnowledgeRelationResult result = new KnowledgeRelationResult(
        "local-template",
        false,
        false,
        false,
        suggestions,
        List.of("已生成本地候选关系，请人工确认后使用。"));
    return new RelationDraft(node, result, boundedSuggestions(node, graph, suggestions, scope), scope);
  }

  private KnowledgeGraphAutoRelateResponse saveDraft(RelationDraft draft, int maxEdges) {
    List<KnowledgeGraphEdge> createdEdges = saveSuggestions(draft.node(), draft.suggestions(), maxEdges);
    List<String> notes = new ArrayList<>();
    notes.add("provider=" + draft.relationResult().provider());
    notes.addAll(draft.relationResult().notes());
    if (draft.scope() == RelationScope.TERTIARY_PARENT_ONLY) {
      notes.add("能力边界：全图编排只补二级栏目 -> 三级内容的归属边，不处理一级 -> 二级固定关系或三级之间证据边。");
    } else if (draft.scope() == RelationScope.TERTIARY_ORCHESTRATE) {
      notes.add("能力边界：全图编排可补二级栏目 -> 三级内容的归属边，也可补三级内容之间的证据边；不处理一级 -> 二级固定关系。");
    } else {
      notes.add("能力边界：每节点最多新增 " + MAX_EDGES_PER_NODE + " 条；只允许相邻层级或同级证据关系；新增边默认隐藏。");
    }
    if (createdEdges.size() < draft.suggestions().size()) {
      notes.add("部分候选关系已存在、无效或被跳过。");
    }
    if (createdEdges.isEmpty()) {
      notes.add("未创建新关系，可在关系管理中手动补充。");
    }
    return new KnowledgeGraphAutoRelateResponse(draft.relationResult().provider(), draft.node(), draft.suggestions(), createdEdges, notes);
  }

  private List<KnowledgeGraphEdge> saveSuggestions(
      KnowledgeGraphNode node,
      List<KnowledgeRelationSuggestion> suggestions,
      int maxEdges) {
    Set<String> nodeKeys = new LinkedHashSet<>(contentService.knowledgeGraphNodes(true).stream().map(KnowledgeGraphNode::nodeKey).toList());
    Set<String> existingEdges = new LinkedHashSet<>(contentService.knowledgeGraphEdges(true).stream().map(this::edgeKey).toList());
    List<KnowledgeGraphEdge> createdEdges = new ArrayList<>();
    int sortOrder = Math.max(500, node.sortOrder());
    for (KnowledgeRelationSuggestion suggestion : suggestions) {
      if (createdEdges.size() >= maxEdges) {
        break;
      }
      if (!nodeKeys.contains(suggestion.fromNodeKey()) || !nodeKeys.contains(suggestion.toNodeKey())) {
        continue;
      }
      if (suggestion.fromNodeKey().equals(suggestion.toNodeKey())) {
        continue;
      }
      KnowledgeGraphEdgeRequest request = new KnowledgeGraphEdgeRequest(
          suggestion.fromNodeKey(),
          suggestion.toNodeKey(),
          relationType(suggestion.relationType()),
          false,
          sortOrder++);
      KnowledgeGraphNode from = contentService.knowledgeGraphNodes(true).stream()
          .filter(item -> item.nodeKey().equals(request.fromNodeKey()))
          .findFirst()
          .orElse(null);
      KnowledgeGraphNode to = contentService.knowledgeGraphNodes(true).stream()
          .filter(item -> item.nodeKey().equals(request.toNodeKey()))
          .findFirst()
          .orElse(null);
      if (from == null || to == null || !KnowledgeGraphHierarchy.edgeAllowed(from, to, request.relationType())) {
        continue;
      }
      String edgeKey = edgeKey(request);
      if (existingEdges.contains(edgeKey)) {
        continue;
      }
      KnowledgeGraphEdge edge = contentService.createKnowledgeGraphEdge(request);
      existingEdges.add(edgeKey);
      createdEdges.add(edge);
    }
    return createdEdges;
  }

  private List<KnowledgeRelationSuggestion> boundedSuggestions(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph,
      List<KnowledgeRelationSuggestion> suggestions,
      RelationScope scope) {
    Map<String, KnowledgeGraphNode> nodeByKey = new LinkedHashMap<>();
    graph.nodes().forEach(item -> nodeByKey.put(item.nodeKey(), item));
    List<KnowledgeRelationSuggestion> accepted = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (KnowledgeRelationSuggestion suggestion : suggestions) {
      KnowledgeGraphNode from = nodeByKey.get(suggestion.fromNodeKey());
      KnowledgeGraphNode to = nodeByKey.get(suggestion.toNodeKey());
      if (from == null || to == null) {
        continue;
      }
      if (!touchesNode(suggestion, node.nodeKey())) {
        continue;
      }
      String relation = relationType(suggestion.relationType());
      if (scope == RelationScope.TERTIARY_PARENT_ONLY && !isTertiaryParentSuggestion(node, from, to, relation)) {
        continue;
      }
      if (scope == RelationScope.TERTIARY_ORCHESTRATE && !isTertiaryOrchestrateSuggestion(node, from, to, relation)) {
        continue;
      }
      if (!relationAllowed(from, to, relation)) {
        continue;
      }
      if ("RELATED".equals(relation) && !hasEvidenceOverlap(from, to)) {
        continue;
      }
      String key = suggestion.fromNodeKey() + "->" + suggestion.toNodeKey() + ":" + relation;
      if (!seen.add(key)) {
        continue;
      }
      accepted.add(new KnowledgeRelationSuggestion(
          suggestion.fromNodeKey(),
          suggestion.toNodeKey(),
          relation,
          trimOrDefault(suggestion.reason(), "符合图谱能力边界的候选关系。")));
      if (accepted.size() >= suggestionLimit(graph, scope)) {
        break;
      }
    }
    return accepted;
  }

  private int suggestionLimit(KnowledgeGraphView graph, RelationScope scope) {
    return switch (scope) {
      case TERTIARY_PARENT_ONLY -> parentSuggestionLimit(graph);
      case TERTIARY_ORCHESTRATE -> parentSuggestionLimit(graph) + MAX_ORCHESTRATE_EVIDENCE_EDGES_PER_NODE;
      case FULL -> MAX_EDGES_PER_NODE;
    };
  }

  private int parentSuggestionLimit(KnowledgeGraphView graph) {
    long sectionCount = graph.nodes().stream().filter(node -> node.level() == 1).count();
    return Math.max(1, Math.toIntExact(Math.min(sectionCount, 8)));
  }

  private Map<String, Long> parentEdgeCountByTertiary(KnowledgeGraphView graph) {
    Map<String, KnowledgeGraphNode> nodeByKey = new LinkedHashMap<>();
    graph.nodes().forEach(node -> nodeByKey.put(node.nodeKey(), node));
    Map<String, Long> counts = new LinkedHashMap<>();
    for (KnowledgeGraphEdge edge : graph.edges()) {
      KnowledgeGraphNode from = nodeByKey.get(edge.fromNodeKey());
      KnowledgeGraphNode to = nodeByKey.get(edge.toNodeKey());
      if (from == null || to == null || !KnowledgeGraphHierarchy.edgeAllowed(from, to, edge.relationType())) {
        continue;
      }
      if (from.level() == 1 && to.level() == 2 && Set.of("INCLUDES", "CONTAINS").contains(relationType(edge.relationType()))) {
        counts.merge(edge.toNodeKey(), 1L, Long::sum);
      }
    }
    return counts;
  }

  private List<KnowledgeRelationSuggestion> fallbackSuggestions(KnowledgeGraphNode node, KnowledgeGraphView graph) {
    List<KnowledgeRelationSuggestion> suggestions = new ArrayList<>(fallbackParentSuggestions(node, graph));
    Set<String> nodeTags = lowerSet(node.tags());
    graph.nodes().stream()
        .filter(item -> !item.nodeKey().equals(node.nodeKey()))
        .filter(item -> item.level() == 2)
        .filter(item -> intersects(nodeTags, lowerSet(item.tags())) || containsAny(node.summary() + " " + node.content(), item.tags()))
        .limit(2)
        .forEach(item -> suggestions.add(new KnowledgeRelationSuggestion(
            node.nodeKey(),
            item.nodeKey(),
            relationFor(node.nodeType(), item.nodeType()),
            "基于标签或摘要关键词匹配。")));
    return suggestions.stream().limit(MAX_EDGES_PER_NODE).toList();
  }

  private List<KnowledgeRelationSuggestion> fallbackParentSuggestions(KnowledgeGraphNode node, KnowledgeGraphView graph) {
    String type = node.nodeType().toUpperCase(Locale.ROOT);
    Set<String> keys = new LinkedHashSet<>(graph.nodes().stream().map(KnowledgeGraphNode::nodeKey).toList());
    List<KnowledgeRelationSuggestion> suggestions = new ArrayList<>();
    if ("SECTION".equals(type) && keys.contains("me")) {
      suggestions.add(new KnowledgeRelationSuggestion("me", node.nodeKey(), "OWNS", "二级栏目归属于个人核心。"));
    } else if ("BLOG".equals(type) && keys.contains("section-blog")) {
      suggestions.add(new KnowledgeRelationSuggestion("section-blog", node.nodeKey(), "CONTAINS", "博客内容归入技术博客栏目。"));
    } else if (("PROJECT".equals(type) || "MODULE".equals(type)) && keys.contains("section-evidence")) {
      suggestions.add(new KnowledgeRelationSuggestion("section-evidence", node.nodeKey(), "INCLUDES", "项目与模块归入项目证据栏目。"));
      if (keys.contains("section-interview")) {
        suggestions.add(new KnowledgeRelationSuggestion("section-interview", node.nodeKey(), "INCLUDES", "项目证据可用于岗位适配与面试讲解。"));
      }
    } else if ("SKILL".equals(type) && keys.contains("section-resume")) {
      suggestions.add(new KnowledgeRelationSuggestion("section-resume", node.nodeKey(), "INCLUDES", "技能节点归入履历能力栏目。"));
    }
    return suggestions.stream().limit(parentSuggestionLimit(graph)).toList();
  }

  private KnowledgeGraphNodeRequest toNodeRequest(KnowledgeGraphSmartNodeRequest request) {
    List<KnowledgeGraphNode> nodes = contentService.knowledgeGraphNodes(true);
    String nodeType = KnowledgeGraphHierarchy.normalizeType(request.nodeType());
    String nodeKey = request.nodeKey() == null || request.nodeKey().isBlank()
        ? uniqueNodeKey(nodeType, request.label(), nodes)
        : request.nodeKey().trim();
    return new KnowledgeGraphNodeRequest(
        nodeKey,
        request.label().trim(),
        nodeType,
        request.level() == null ? defaultLevel(nodeType) : request.level(),
        trimOrDefault(request.summary(), request.label().trim()),
        trimOrDefault(request.content(), trimOrDefault(request.summary(), request.label().trim())),
        request.tags() == null ? List.of() : request.tags(),
        trimOrEmpty(request.href()),
        trimOrDefault(request.sourceType(), "MANUAL"),
        trimOrEmpty(request.sourceSlug()),
        request.x() == null ? 0 : request.x(),
        request.y() == null ? 0 : request.y(),
        request.z() == null ? 0 : request.z(),
        request.visible() == null || request.visible(),
        request.sortOrder() == null ? nextSortOrder(nodes, nodeType) : request.sortOrder());
  }

  private KnowledgeGraphView graphIncludingHidden() {
    return new KnowledgeGraphView(contentService.knowledgeGraphNodes(true), contentService.knowledgeGraphEdges(true));
  }

  private String uniqueNodeKey(String nodeType, String label, List<KnowledgeGraphNode> nodes) {
    Set<String> existing = new LinkedHashSet<>(nodes.stream().map(KnowledgeGraphNode::nodeKey).toList());
    String base = nodeType.toLowerCase(Locale.ROOT) + "-" + slug(label);
    String candidate = base;
    int index = 2;
    while (existing.contains(candidate)) {
      candidate = base + "-" + index++;
    }
    return candidate;
  }

  private String slug(String value) {
    String normalized = Normalizer.normalize(value == null ? "node" : value, Normalizer.Form.NFKD)
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
        .replaceAll("(^-|-$)", "");
    if (normalized.isBlank()) {
      return "node";
    }
    return normalized.length() > 64 ? normalized.substring(0, 64).replaceAll("-$", "") : normalized;
  }

  private int defaultLevel(String nodeType) {
    return KnowledgeGraphHierarchy.expectedLevel(nodeType);
  }

  private int nextSortOrder(List<KnowledgeGraphNode> nodes, String nodeType) {
    return nodes.stream()
        .filter(node -> node.nodeType().equalsIgnoreCase(nodeType))
        .mapToInt(KnowledgeGraphNode::sortOrder)
        .max()
        .orElse(500) + 10;
  }

  private String relationType(String relationType) {
    if (relationType == null || relationType.isBlank()) {
      return "RELATED";
    }
    return relationType.trim().toUpperCase(Locale.ROOT);
  }

  private boolean touchesNode(KnowledgeRelationSuggestion suggestion, String nodeKey) {
    return nodeKey.equals(suggestion.fromNodeKey()) || nodeKey.equals(suggestion.toNodeKey());
  }

  private boolean relationAllowed(KnowledgeGraphNode from, KnowledgeGraphNode to, String relation) {
    return KnowledgeGraphHierarchy.edgeAllowed(from, to, relation);
  }

  private boolean isTertiaryParentSuggestion(
      KnowledgeGraphNode current,
      KnowledgeGraphNode from,
      KnowledgeGraphNode to,
      String relation) {
    return current.nodeKey().equals(to.nodeKey())
        && current.level() == 2
        && from.level() == 1
        && to.level() == 2
        && Set.of("INCLUDES", "CONTAINS").contains(relation);
  }

  private boolean isTertiaryOrchestrateSuggestion(
      KnowledgeGraphNode current,
      KnowledgeGraphNode from,
      KnowledgeGraphNode to,
      String relation) {
    if (isTertiaryParentSuggestion(current, from, to, relation)) {
      return true;
    }
    return current.level() == 2
        && from.level() == 2
        && to.level() == 2
        && touchesNode(new KnowledgeRelationSuggestion(from.nodeKey(), to.nodeKey(), relation, ""), current.nodeKey())
        && Set.of("USES", "EXPLAINS", "RELATED").contains(relation);
  }

  private boolean hasEvidenceOverlap(KnowledgeGraphNode from, KnowledgeGraphNode to) {
    Set<String> left = lowerSet(from.tags());
    Set<String> right = lowerSet(to.tags());
    if (left.stream().anyMatch(right::contains)) {
      return true;
    }
    String fromText = (from.summary() + " " + from.content()).toLowerCase(Locale.ROOT);
    String toText = (to.summary() + " " + to.content()).toLowerCase(Locale.ROOT);
    return left.stream().anyMatch(toText::contains) || right.stream().anyMatch(fromText::contains);
  }

  private String relationFor(String fromType, String toType) {
    String normalizedFrom = KnowledgeGraphHierarchy.normalizeType(fromType);
    String normalizedTo = KnowledgeGraphHierarchy.normalizeType(toType);
    if ("BLOG".equals(normalizedFrom) || "BLOG".equals(normalizedTo)) {
      return "EXPLAINS";
    }
    if ("PROJECT".equals(normalizedFrom) || "MODULE".equals(normalizedFrom)) {
      return "USES";
    }
    return "RELATED";
  }

  private boolean intersects(Set<String> left, Set<String> right) {
    return left.stream().anyMatch(right::contains);
  }

  private boolean containsAny(String text, List<String> values) {
    if (text == null || values == null || values.isEmpty()) {
      return false;
    }
    String normalized = text.toLowerCase(Locale.ROOT);
    return values.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .anyMatch(normalized::contains);
  }

  private Set<String> lowerSet(List<String> values) {
    if (values == null) {
      return Set.of();
    }
    Set<String> result = new LinkedHashSet<>();
    values.forEach(value -> {
      if (value != null && !value.isBlank()) {
        result.add(value.toLowerCase(Locale.ROOT));
      }
    });
    return result;
  }

  private String edgeKey(KnowledgeGraphEdge edge) {
    return edge.fromNodeKey() + "->" + edge.toNodeKey() + ":" + edge.relationType().toUpperCase(Locale.ROOT);
  }

  private String edgeKey(KnowledgeGraphEdgeRequest request) {
    return request.fromNodeKey() + "->" + request.toNodeKey() + ":" + relationType(request.relationType());
  }

  private String trimOrDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String trimOrEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private enum RelationScope {
    FULL,
    TERTIARY_PARENT_ONLY,
    TERTIARY_ORCHESTRATE
  }

  private record RelationDraft(
      KnowledgeGraphNode node,
      KnowledgeRelationResult relationResult,
      List<KnowledgeRelationSuggestion> suggestions,
      RelationScope scope) {
  }
}
