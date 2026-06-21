package com.mystyle.portfolio.llm;

import java.util.List;

public record MockInterviewResponse(
    String provider,
    String role,
    List<MockInterviewQuestion> questions,
    String closingAdvice,
    List<String> riskNotes) {

  public record MockInterviewQuestion(
      String question,
      String intent,
      String strongAnswer,
      List<String> followUps,
      List<String> scoreFocus) {
  }
}
