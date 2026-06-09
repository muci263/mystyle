package com.mystyle.portfolio.llm;

import java.util.List;

public record ResumeOptimizeResponse(
    String provider,
    String role,
    String summary,
    List<String> highlights,
    List<String> riskNotes) {
}
