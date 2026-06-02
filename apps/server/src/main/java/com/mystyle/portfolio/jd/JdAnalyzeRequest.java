package com.mystyle.portfolio.jd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record JdAnalyzeRequest(
    @NotBlank @Size(max = 8000) String jd,
    List<String> target,
    String variantName) {
}
