package com.mystyle.portfolio.blog;

import com.mystyle.portfolio.common.ApiException;
import java.util.Arrays;

public enum BlogCategoryType {
  INTERNSHIP("实习心得"),
  AI_LEARNING("AI学习"),
  BACKEND_PRACTICE("后端实践"),
  PROJECT_REVIEW("项目复盘"),
  DEVOPS("工程部署"),
  INTERVIEW_REVIEW("面试复盘");

  private final String label;

  BlogCategoryType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public boolean matches(String value) {
    return name().equalsIgnoreCase(value) || label.equalsIgnoreCase(value);
  }

  public static BlogCategoryType from(String value) {
    if (value == null || value.isBlank()) {
      throw ApiException.badRequest("博客分类不能为空");
    }
    return Arrays.stream(values())
        .filter(type -> type.matches(value.trim()))
        .findFirst()
        .orElseThrow(() -> ApiException.badRequest("博客分类不在枚举范围内"));
  }
}
