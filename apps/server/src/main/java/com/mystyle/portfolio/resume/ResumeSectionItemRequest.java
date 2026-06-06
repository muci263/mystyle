package com.mystyle.portfolio.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ResumeSectionItemRequest(
    @NotBlank @Size(max = 160) String title,
    @Size(max = 160) String subtitle,
    @Size(max = 64) String period,
    String summary,
    String detail,
    List<String> tags,
    Boolean visible,
    Integer sortOrder) {
}
