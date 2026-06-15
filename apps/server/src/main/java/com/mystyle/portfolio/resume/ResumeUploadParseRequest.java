package com.mystyle.portfolio.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeUploadParseRequest(
    @NotBlank @Size(max = 255) String filename,
    @Size(max = 128) String contentType,
    @NotBlank String content,
    Boolean allowFallback) {
}
