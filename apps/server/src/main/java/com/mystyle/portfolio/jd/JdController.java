package com.mystyle.portfolio.jd;

import com.mystyle.portfolio.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jd")
public class JdController {
  private final JdAnalysisService analysisService;

  public JdController(JdAnalysisService analysisService) {
    this.analysisService = analysisService;
  }

  @PostMapping("/analyze")
  public ApiResponse<JdAnalysisResponse> analyze(@Valid @RequestBody JdAnalyzeRequest request) {
    return ApiResponse.success(analysisService.analyze(request));
  }

  @PostMapping("/analyses/{analysisId}/variant")
  public ApiResponse<ProfileVariantResponse> createVariant(@PathVariable("analysisId") long analysisId) {
    return ApiResponse.success(analysisService.createVariant(analysisId));
  }
}
