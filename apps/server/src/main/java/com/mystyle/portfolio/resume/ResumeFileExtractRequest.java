package com.mystyle.portfolio.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeFileExtractRequest(
    @NotBlank @Size(max = 255) String filename,
    @Size(max = 128) String contentType,
    @NotBlank @Size(max = 8_000_000) String contentBase64) {
}
