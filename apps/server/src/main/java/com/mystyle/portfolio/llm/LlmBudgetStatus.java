package com.mystyle.portfolio.llm;

public record LlmBudgetStatus(
    String date,
    String zoneId,
    boolean enabled,
    double dailyLimitRmb,
    double usedRmb,
    double reservedRmb,
    double remainingRmb,
    double inputPriceRmbPerMillionTokens,
    double outputPriceRmbPerMillionTokens) {
}
