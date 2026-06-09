package com.mystyle.portfolio.llm;

public record LlmProviderStatus(
    String provider,
    boolean configured,
    String textModel,
    String visionModel,
    String mode) {
}
