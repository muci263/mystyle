package com.mystyle.portfolio.analytics;

public record AnalyticsEventResponse(
    long eventId,
    String eventType,
    String status) {
}
