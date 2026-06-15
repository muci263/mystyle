package com.mystyle.portfolio.knowledge;

import com.mystyle.portfolio.common.ApiResponse;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphEdge;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphView;
import com.mystyle.portfolio.content.PortfolioContentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KnowledgeGraphController {
  private final PortfolioContentService contentService;
  private final KnowledgeGraphSmartService smartService;

  public KnowledgeGraphController(PortfolioContentService contentService, KnowledgeGraphSmartService smartService) {
    this.contentService = contentService;
    this.smartService = smartService;
  }

  @GetMapping("/public/knowledge-graph")
  public ApiResponse<KnowledgeGraphView> publicGraph() {
    return ApiResponse.success(contentService.knowledgeGraph());
  }

  @GetMapping("/admin/knowledge-graph/nodes")
  public ApiResponse<List<KnowledgeGraphNode>> nodes(
      @RequestParam(value = "includeHidden", defaultValue = "true") boolean includeHidden) {
    return ApiResponse.success(contentService.knowledgeGraphNodes(includeHidden));
  }

  @PostMapping("/admin/knowledge-graph/nodes")
  public ApiResponse<KnowledgeGraphNode> createNode(@Valid @RequestBody KnowledgeGraphNodeRequest request) {
    return ApiResponse.success(contentService.createKnowledgeGraphNode(request));
  }

  @PostMapping("/admin/knowledge-graph/nodes/smart-create")
  public ApiResponse<KnowledgeGraphAutoRelateResponse> smartCreateNode(
      @Valid @RequestBody KnowledgeGraphSmartNodeRequest request,
      @RequestParam(value = "allowFallback", defaultValue = "false") boolean allowFallback) {
    return ApiResponse.success(smartService.smartCreate(request, allowFallback));
  }

  @PostMapping("/admin/knowledge-graph/nodes/{nodeKey}/auto-relate")
  public ApiResponse<KnowledgeGraphAutoRelateResponse> autoRelateNode(
      @PathVariable("nodeKey") String nodeKey,
      @RequestParam(value = "allowFallback", defaultValue = "false") boolean allowFallback) {
    return ApiResponse.success(smartService.autoRelate(nodeKey, allowFallback));
  }

  @PostMapping("/admin/knowledge-graph/orchestrate")
  public ApiResponse<KnowledgeGraphOrchestrateResponse> orchestrateGraph(
      @RequestParam(value = "allowFallback", defaultValue = "false") boolean allowFallback) {
    return ApiResponse.success(smartService.orchestrate(allowFallback));
  }

  @PutMapping("/admin/knowledge-graph/nodes/{nodeKey}")
  public ApiResponse<KnowledgeGraphNode> updateNode(
      @PathVariable("nodeKey") String nodeKey,
      @Valid @RequestBody KnowledgeGraphNodeRequest request) {
    return ApiResponse.success(contentService.updateKnowledgeGraphNode(nodeKey, request));
  }

  @DeleteMapping("/admin/knowledge-graph/nodes/{nodeKey}")
  public ApiResponse<String> deleteNode(@PathVariable("nodeKey") String nodeKey) {
    contentService.deleteKnowledgeGraphNode(nodeKey);
    return ApiResponse.success("DELETED");
  }

  @GetMapping("/admin/knowledge-graph/edges")
  public ApiResponse<List<KnowledgeGraphEdge>> edges(
      @RequestParam(value = "includeHidden", defaultValue = "true") boolean includeHidden) {
    return ApiResponse.success(contentService.knowledgeGraphEdges(includeHidden));
  }

  @PostMapping("/admin/knowledge-graph/edges")
  public ApiResponse<KnowledgeGraphEdge> createEdge(@Valid @RequestBody KnowledgeGraphEdgeRequest request) {
    return ApiResponse.success(contentService.createKnowledgeGraphEdge(request));
  }

  @PutMapping("/admin/knowledge-graph/edges/{edgeId}")
  public ApiResponse<KnowledgeGraphEdge> updateEdge(
      @PathVariable("edgeId") long edgeId,
      @Valid @RequestBody KnowledgeGraphEdgeRequest request) {
    return ApiResponse.success(contentService.updateKnowledgeGraphEdge(edgeId, request));
  }

  @DeleteMapping("/admin/knowledge-graph/edges/{edgeId}")
  public ApiResponse<String> deleteEdge(@PathVariable("edgeId") long edgeId) {
    contentService.deleteKnowledgeGraphEdge(edgeId);
    return ApiResponse.success("DELETED");
  }
}
