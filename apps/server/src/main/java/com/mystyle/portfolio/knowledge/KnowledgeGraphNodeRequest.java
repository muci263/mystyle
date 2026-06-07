package com.mystyle.portfolio.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record KnowledgeGraphNodeRequest(
    @NotBlank @Size(max = 128) String nodeKey,
    @NotBlank @Size(max = 160) String label,
    @NotBlank @Size(max = 32) String nodeType,
    int level,
    @Size(max = 1000) String summary,
    @Size(max = 4000) String content,
    List<@Size(max = 64) String> tags,
    @Size(max = 255) String href,
    @Size(max = 32) String sourceType,
    @Size(max = 128) String sourceSlug,
    double x,
    double y,
    double z,
    boolean visible,
    int sortOrder) {
}
