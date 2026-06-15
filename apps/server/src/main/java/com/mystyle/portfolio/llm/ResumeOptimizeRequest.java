package com.mystyle.portfolio.llm;

import jakarta.validation.constraints.Size;
import java.util.List;

public record ResumeOptimizeRequest(
    @Size(max = 12000) String resumeText,
    @Size(max = 12000) String jdText,
    List<@Size(max = 256) String> assetHints,
    @Size(max = 64) String targetRole,
    Boolean allowFallback) {
}
