package com.mystyle.portfolio.moduleDemo;

import com.mystyle.portfolio.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lab/video-learning")
public class VideoLearningController {
  private final VideoLearningService videoLearningService;

  public VideoLearningController(VideoLearningService videoLearningService) {
    this.videoLearningService = videoLearningService;
  }

  @PostMapping("/reset")
  public ApiResponse<VideoLearningSnapshot> reset() {
    return ApiResponse.success(videoLearningService.reset());
  }

  @PostMapping("/progress")
  public ApiResponse<VideoLearningSnapshot> progress(@Valid @RequestBody VideoProgressRequest request) {
    return ApiResponse.success(videoLearningService.progress(request));
  }

  @PostMapping("/complete")
  public ApiResponse<VideoLearningSnapshot> complete() {
    return ApiResponse.success(videoLearningService.complete());
  }

  @GetMapping("/snapshot")
  public ApiResponse<VideoLearningSnapshot> snapshot() {
    return ApiResponse.success(videoLearningService.snapshot());
  }
}
