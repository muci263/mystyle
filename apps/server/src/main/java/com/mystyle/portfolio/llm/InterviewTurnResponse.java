package com.mystyle.portfolio.llm;

import java.util.List;

public record InterviewTurnResponse(
    String provider,
    String role,
    int round,
    String question,
    String intent,
    List<String> followUps,
    List<String> scoreFocus,
    boolean canFinish,
    List<String> notes) {
}
