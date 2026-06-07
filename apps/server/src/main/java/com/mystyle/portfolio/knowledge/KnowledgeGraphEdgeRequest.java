package com.mystyle.portfolio.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeGraphEdgeRequest(
    @NotBlank @Size(max = 128) String fromNodeKey,
    @NotBlank @Size(max = 128) String toNodeKey,
    @Size(max = 64) String relationType,
    boolean visible,
    int sortOrder) {
}
