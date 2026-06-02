package com.mystyle.portfolio.project;

import com.mystyle.portfolio.common.ApiResponse;
import com.mystyle.portfolio.content.ContentModels.Project;
import com.mystyle.portfolio.content.PortfolioContentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/projects")
public class ProjectController {
  private final PortfolioContentService contentService;

  public ProjectController(PortfolioContentService contentService) {
    this.contentService = contentService;
  }

  @GetMapping
  public ApiResponse<List<Project>> list(@RequestParam(value = "tech", required = false) String tech) {
    return ApiResponse.success(contentService.projects(tech));
  }

  @GetMapping("/{slug}")
  public ApiResponse<Project> detail(@PathVariable("slug") String slug) {
    return ApiResponse.success(contentService.project(slug));
  }
}
