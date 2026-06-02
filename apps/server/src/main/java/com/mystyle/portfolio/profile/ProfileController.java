package com.mystyle.portfolio.profile;

import com.mystyle.portfolio.common.ApiResponse;
import com.mystyle.portfolio.content.ContentModels.HomeView;
import com.mystyle.portfolio.content.PortfolioContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class ProfileController {
  private final PortfolioContentService contentService;

  public ProfileController(PortfolioContentService contentService) {
    this.contentService = contentService;
  }

  @GetMapping("/home")
  public ApiResponse<HomeView> home() {
    return ApiResponse.success(contentService.home());
  }
}
