package com.mystyle.portfolio.jd;

import java.util.List;

public record JdAnalysisResponse(
    long analysisId,
    String provider,
    String role,
    List<String> keywords,
    int matchScore,
    String summary,
    List<ProjectRecommendation> projectRecommendations,
    List<ModuleRecommendation> moduleRecommendations,
    List<String> resumeOptimizations,
    List<String> interviewTalkingPoints,
    List<String> riskNotes) {

  public record ProjectRecommendation(
      String slug,
      String name,
      String emphasis,
      List<String> supportedBy) {
  }

  public record ModuleRecommendation(
      String slug,
      String title,
      String reason) {
  }
}
