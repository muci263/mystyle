package com.mystyle.portfolio.moduleDemo;

import java.util.List;

public record AgentWorkflowRun(
    String runId,
    String status,
    String question,
    String answer,
    List<AgentWorkflowStep> steps) {
}
