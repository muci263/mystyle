package com.mystyle.portfolio.llm;

import jakarta.validation.constraints.Size;

public record KnowledgeRelationSuggestion(
    @Size(max = 128) String fromNodeKey,
    @Size(max = 128) String toNodeKey,
    @Size(max = 64) String relationType,
    @Size(max = 500) String reason) {
}
