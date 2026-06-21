package com.mystyle.portfolio.llm;

import jakarta.validation.constraints.Size;
import java.util.List;

public record MockInterviewRequest(
    @Size(max = 12000) String resumeText,
    @Size(max = 12000) String jdText,
    @Size(max = 64) String targetRole,
    List<@Size(max = 256) String> assetHints,
    Boolean allowFallback) {
}
