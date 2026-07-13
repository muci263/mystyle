package com.mystyle.portfolio.llm;

import java.util.List;

public record InterviewFinalizeResponse(
    String provider,
    String role,
    String summary,
    String generatedResumeMarkdown,
    List<String> highlights,
    List<String> resumeSuggestions,
    List<String> interviewFeedback,
    List<String> nextActions,
    List<String> riskNotes,
    int questionCount) {
}
