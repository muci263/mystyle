package com.mystyle.portfolio.health;

import com.mystyle.portfolio.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  private final HealthService healthService;

  public HealthController(HealthService healthService) {
    this.healthService = healthService;
  }

  @GetMapping("/health")
  public ApiResponse<HealthStatus> health() {
    return ApiResponse.success(healthService.health());
  }
}
