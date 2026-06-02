package com.mystyle.portfolio.moduleDemo;

import com.mystyle.portfolio.common.ApiResponse;
import com.mystyle.portfolio.content.ContentModels.ModuleDemo;
import com.mystyle.portfolio.content.PortfolioContentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/module-demos")
public class ModuleDemoController {
  private final PortfolioContentService contentService;

  public ModuleDemoController(PortfolioContentService contentService) {
    this.contentService = contentService;
  }

  @GetMapping
  public ApiResponse<List<ModuleDemo>> list(@RequestParam(value = "tech", required = false) String tech) {
    return ApiResponse.success(contentService.moduleDemos(tech));
  }

  @GetMapping("/{slug}")
  public ApiResponse<ModuleDemo> detail(@PathVariable("slug") String slug) {
    return ApiResponse.success(contentService.moduleDemo(slug));
  }
}
