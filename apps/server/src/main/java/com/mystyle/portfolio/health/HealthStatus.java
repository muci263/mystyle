package com.mystyle.portfolio.health;

import java.util.Map;

public record HealthStatus(
    String status,
    String application,
    Map<String, String> components) {
}
