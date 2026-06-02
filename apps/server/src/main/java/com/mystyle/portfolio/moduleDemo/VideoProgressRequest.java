package com.mystyle.portfolio.moduleDemo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record VideoProgressRequest(
    @NotBlank String userId,
    @NotBlank String courseId,
    @NotBlank String lessonId,
    @Min(0) int currentSecond,
    @Min(1) int durationSecond) {
}
