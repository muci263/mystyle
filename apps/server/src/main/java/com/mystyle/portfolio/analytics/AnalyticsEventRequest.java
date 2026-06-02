package com.mystyle.portfolio.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AnalyticsEventRequest(
    @NotBlank String eventType,
    @Size(max = 256) String path,
    Map<String, Object> payload) {
}
