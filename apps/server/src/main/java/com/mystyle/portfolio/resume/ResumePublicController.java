package com.mystyle.portfolio.resume;

import com.mystyle.portfolio.common.ApiResponse;
import com.mystyle.portfolio.resume.ResumeModels.ResumeDraftView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/resume-content")
public class ResumePublicController {
  private final ResumeAdminService resumeAdminService;

  public ResumePublicController(ResumeAdminService resumeAdminService) {
    this.resumeAdminService = resumeAdminService;
  }

  @GetMapping
  public ApiResponse<ResumeDraftView> publicResume() {
    return ApiResponse.success(resumeAdminService.publicResume());
  }
}
