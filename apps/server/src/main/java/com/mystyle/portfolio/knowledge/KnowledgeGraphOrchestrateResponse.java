package com.mystyle.portfolio.knowledge;

import java.util.List;

public record KnowledgeGraphOrchestrateResponse(
    String provider,
    int scannedNodes,
    int createdEdges,
    List<KnowledgeGraphAutoRelateResponse> results,
    List<String> notes) {
}
