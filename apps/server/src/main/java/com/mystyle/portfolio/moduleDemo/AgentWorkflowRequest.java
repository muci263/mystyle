package com.mystyle.portfolio.moduleDemo;

import jakarta.validation.constraints.NotBlank;

public record AgentWorkflowRequest(
    @NotBlank String question,
    String templateId,
    String memory) {
}
