package com.mystyle.portfolio.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlogAnnotationRequest(
    @NotBlank @Size(max = 160) String anchorText,
    @NotBlank @Size(max = 1200) String note) {
}
