package com.mystyle.portfolio.knowledge;

import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphEdge;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphView;
import com.mystyle.portfolio.content.PortfolioContentService;
import com.mystyle.portfolio.llm.KnowledgeRelationSuggestion;
import com.mystyle.portfolio.llm.LlmService;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeGraphSmartService {
  private final PortfolioContentService contentService;
  private final LlmService llmService;

  public KnowledgeGraphSmartService(PortfolioContentService contentService, LlmService llmService) {
    this.contentService = contentService;
    this.llmService = llmService;
  }

  public KnowledgeGraphAutoRelateResponse smartCreate(KnowledgeGraphSmartNodeRequest request) {
    KnowledgeGraphNodeRequest nodeRequest = toNodeRequest(request);
    KnowledgeGraphNode createdNode = contentService.createKnowledgeGraphNode(nodeRequest);
    return autoRelate(createdNode.nodeKey());
  }

  public KnowledgeGraphAutoRelateResponse autoRelate(String nodeKey) {
    KnowledgeGraphNode node = contentService.knowledgeGraphNodes(true).stream()
        .filter(item -> item.nodeKey().equals(nodeKey))
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("图谱节点不存在"));
    KnowledgeGraphView graph = graphIncludingHidden();
    List<KnowledgeRelationSuggestion> suggestions = llmService.suggestKnowledgeRelations(node, graph);
    List<KnowledgeGraphEdge> createdEdges = saveSuggestions(node, suggestions);
    List<String> notes = new ArrayList<>();
    notes.add("provider=" + llmService.providerName());
    if (createdEdges.size() < suggestions.size()) {
      notes.add("部分候选关系已存在、无效或被跳过。");
    }
    if (createdEdges.isEmpty()) {
      notes.add("未创建新关系，可在关系 CRUD 中手动补充。");
    }
    return new KnowledgeGraphAutoRelateResponse(llmService.providerName(), node, suggestions, createdEdges, notes);
  }

  private List<KnowledgeGraphEdge> saveSuggestions(KnowledgeGraphNode node, List<KnowledgeRelationSuggestion> suggestions) {
    Set<String> nodeKeys = new LinkedHashSet<>(contentService.knowledgeGraphNodes(true).stream().map(KnowledgeGraphNode::nodeKey).toList());
    Set<String> existingEdges = new LinkedHashSet<>(contentService.knowledgeGraphEdges(true).stream().map(this::edgeKey).toList());
    List<KnowledgeGraphEdge> createdEdges = new ArrayList<>();
    int sortOrder = Math.max(500, node.sortOrder());
    for (KnowledgeRelationSuggestion suggestion : suggestions) {
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
          true,
          sortOrder++);
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

  private KnowledgeGraphNodeRequest toNodeRequest(KnowledgeGraphSmartNodeRequest request) {
    List<KnowledgeGraphNode> nodes = contentService.knowledgeGraphNodes(true);
    String nodeType = request.nodeType().trim().toUpperCase(Locale.ROOT);
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
    return switch (nodeType) {
      case "CORE" -> 0;
      case "SECTION" -> 1;
      default -> 2;
    };
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
}
