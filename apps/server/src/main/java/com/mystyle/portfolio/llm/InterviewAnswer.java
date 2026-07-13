package com.mystyle.portfolio.llm;

import jakarta.validation.constraints.Size;
import java.util.List;

public record InterviewAnswer(
    @Size(max = 800) String question,
    @Size(max = 4000) String answer,
    @Size(max = 800) String intent,
    List<@Size(max = 160) String> scoreFocus) {
}
