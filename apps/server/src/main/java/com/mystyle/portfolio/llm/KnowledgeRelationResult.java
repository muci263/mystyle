package com.mystyle.portfolio.llm;

import java.util.List;

public record KnowledgeRelationResult(
    String provider,
    boolean configured,
    boolean modelAttempted,
    boolean modelSucceeded,
    List<KnowledgeRelationSuggestion> suggestions,
    List<String> notes) {
}
