package com.mystyle.portfolio.resume;

import com.mystyle.portfolio.common.ApiException;
import java.util.Locale;

public enum ResumeSectionType {
  SKILL,
  AWARD,
  INTERNSHIP,
  PROJECT,
  ADVANTAGE;

  public static ResumeSectionType from(String value) {
    if (value == null || value.isBlank()) {
      throw ApiException.badRequest("履历板块不能为空");
    }
    try {
      return ResumeSectionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw ApiException.badRequest("不支持的履历板块: " + value);
    }
  }
}
