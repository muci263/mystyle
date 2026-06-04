package com.mystyle.portfolio.blog;

import com.mystyle.portfolio.common.ApiResponse;
import com.mystyle.portfolio.content.ContentModels.BlogAnnotation;
import com.mystyle.portfolio.content.ContentModels.BlogCategory;
import com.mystyle.portfolio.content.ContentModels.BlogComment;
import com.mystyle.portfolio.content.ContentModels.BlogInteractionSummary;
import com.mystyle.portfolio.content.ContentModels.BlogPost;
import com.mystyle.portfolio.content.PortfolioContentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/blog-posts")
public class BlogController {
  private final PortfolioContentService contentService;

  public BlogController(PortfolioContentService contentService) {
    this.contentService = contentService;
  }

  @GetMapping
  public ApiResponse<List<BlogPost>> list(
      @RequestParam(value = "category", required = false) String category,
      @RequestParam(value = "tag", required = false) String tag) {
    return ApiResponse.success(contentService.blogPosts(category, tag));
  }

  @PostMapping
  public ApiResponse<BlogPost> create(@Valid @RequestBody BlogPostRequest request) {
    return ApiResponse.success(contentService.createBlogPost(request));
  }

  @PutMapping("/{slug}")
  public ApiResponse<BlogPost> update(
      @PathVariable("slug") String slug,
      @Valid @RequestBody BlogPostRequest request) {
    return ApiResponse.success(contentService.updateBlogPost(slug, request));
  }

  @GetMapping("/categories")
  public ApiResponse<List<BlogCategory>> categories() {
    return ApiResponse.success(contentService.blogCategories());
  }

  @GetMapping("/{slug}")
  public ApiResponse<BlogPost> detail(@PathVariable("slug") String slug) {
    return ApiResponse.success(contentService.blogPost(slug));
  }

  @GetMapping("/{slug}/comments")
  public ApiResponse<List<BlogComment>> comments(@PathVariable("slug") String slug) {
    return ApiResponse.success(contentService.blogComments(slug));
  }

  @PostMapping("/{slug}/comments")
  public ApiResponse<BlogComment> addComment(
      @PathVariable("slug") String slug,
      @Valid @RequestBody BlogCommentRequest request) {
    return ApiResponse.success(contentService.addBlogComment(slug, request));
  }

  @GetMapping("/{slug}/annotations")
  public ApiResponse<List<BlogAnnotation>> annotations(@PathVariable("slug") String slug) {
    return ApiResponse.success(contentService.blogAnnotations(slug));
  }

  @PostMapping("/{slug}/annotations")
  public ApiResponse<BlogAnnotation> addAnnotation(
      @PathVariable("slug") String slug,
      @Valid @RequestBody BlogAnnotationRequest request) {
    return ApiResponse.success(contentService.addBlogAnnotation(slug, request));
  }

  @PostMapping("/{slug}/likes")
  public ApiResponse<BlogInteractionSummary> like(@PathVariable("slug") String slug) {
    return ApiResponse.success(contentService.likeBlogPost(slug));
  }

  @GetMapping("/{slug}/interactions")
  public ApiResponse<BlogInteractionSummary> interactions(@PathVariable("slug") String slug) {
    return ApiResponse.success(contentService.blogInteractionSummary(slug));
  }
}
