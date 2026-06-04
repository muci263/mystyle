package com.mystyle.portfolio.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlogCommentRequest(
    @NotBlank @Size(max = 64) String author,
    @NotBlank @Size(max = 1000) String content) {
}
