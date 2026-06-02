package com.mystyle.portfolio.jd;

import java.util.List;

public record ProfileVariantResponse(
    long variantId,
    long analysisId,
    String variantName,
    String intro,
    List<String> projectOrder,
    List<String> moduleOrder,
    String status) {
}
