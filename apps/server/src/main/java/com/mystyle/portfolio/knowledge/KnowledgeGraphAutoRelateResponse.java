package com.mystyle.portfolio.knowledge;

import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphEdge;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import com.mystyle.portfolio.llm.KnowledgeRelationSuggestion;
import java.util.List;

public record KnowledgeGraphAutoRelateResponse(
    String provider,
    KnowledgeGraphNode node,
    List<KnowledgeRelationSuggestion> suggestions,
    List<KnowledgeGraphEdge> createdEdges,
    List<String> notes) {
}
