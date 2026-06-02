package com.mystyle.portfolio.analytics;

import com.mystyle.portfolio.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
  private final AnalyticsService analyticsService;

  public AnalyticsController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @PostMapping("/events")
  public ApiResponse<AnalyticsEventResponse> record(@Valid @RequestBody AnalyticsEventRequest request) {
    return ApiResponse.success(analyticsService.record(request));
  }
}
