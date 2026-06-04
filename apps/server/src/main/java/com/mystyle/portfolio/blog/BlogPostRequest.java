package com.mystyle.portfolio.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BlogPostRequest(
    @NotBlank @Size(max = 160) String title,
    @NotBlank @Size(max = 1000) String excerpt,
    @NotBlank @Size(max = 12000) String content,
    @NotBlank @Size(max = 64) String category,
    List<@Size(max = 64) String> tags,
    Integer readMinutes) {
}
