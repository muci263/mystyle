package com.mystyle.portfolio.resume;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeBasicInfoRequest(
    @NotBlank @Size(max = 64) String name,
    @NotBlank @Size(max = 128) String title,
    @NotBlank String summary,
    @Email @Size(max = 128) String email,
    @Size(max = 64) String phone,
    @Size(max = 128) String location,
    @NotBlank @Size(max = 128) String education,
    @Size(max = 255) String githubUrl,
    @Size(max = 255) String websiteUrl) {
}
