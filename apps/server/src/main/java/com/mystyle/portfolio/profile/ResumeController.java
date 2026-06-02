package com.mystyle.portfolio.profile;

import com.mystyle.portfolio.common.ApiResponse;
import com.mystyle.portfolio.content.ContentModels.InterviewGuide;
import com.mystyle.portfolio.content.ContentModels.ResumeView;
import com.mystyle.portfolio.content.ContentModels.TimelineItem;
import com.mystyle.portfolio.content.PortfolioContentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class ResumeController {
  private final PortfolioContentService contentService;

  public ResumeController(PortfolioContentService contentService) {
    this.contentService = contentService;
  }

  @GetMapping("/resume")
  public ApiResponse<ResumeView> resume() {
    return ApiResponse.success(contentService.resume());
  }

  @GetMapping("/timeline")
  public ApiResponse<List<TimelineItem>> timeline() {
    return ApiResponse.success(contentService.timeline());
  }

  @GetMapping("/interview")
  public ApiResponse<InterviewGuide> interview() {
    return ApiResponse.success(contentService.interviewGuide());
  }
}
