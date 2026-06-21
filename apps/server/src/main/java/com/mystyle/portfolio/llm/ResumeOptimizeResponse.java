package com.mystyle.portfolio.llm;

import java.util.List;

public record ResumeOptimizeResponse(
    String provider,
    String role,
    String summary,
    String rewrittenSummary,
    String generatedResumeMarkdown,
    List<String> highlights,
    List<String> sectionSuggestions,
    List<String> riskNotes) {
}
