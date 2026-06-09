package com.mystyle.portfolio.llm;

import com.mystyle.portfolio.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/llm")
public class LlmController {
  private final LlmService llmService;

  public LlmController(LlmService llmService) {
    this.llmService = llmService;
  }

  @GetMapping("/status")
  public ApiResponse<LlmProviderStatus> status() {
    return ApiResponse.success(llmService.status());
  }

  @PostMapping("/resume/optimize")
  public ApiResponse<ResumeOptimizeResponse> optimizeResume(@Valid @RequestBody ResumeOptimizeRequest request) {
    return ApiResponse.success(llmService.optimizeResume(request));
  }
}
