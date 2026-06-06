package com.mystyle.portfolio.resume;

import java.util.List;
import java.util.Map;

public final class ResumeModels {
  private ResumeModels() {
  }

  public record ResumeVersion(
      long id,
      String versionName,
      String status,
      Long sourceTaskId,
      String publishedAt,
      String createdAt,
      String updatedAt) {
  }

  public record ResumeBasicInfo(
      long id,
      long versionId,
      String name,
      String title,
      String summary,
      String email,
      String phone,
      String location,
      String education,
      String githubUrl,
      String websiteUrl,
      String updatedAt) {
  }

  public record ResumeSectionItem(
      long id,
      long versionId,
      ResumeSectionType sectionType,
      String title,
      String subtitle,
      String period,
      String summary,
      String detail,
      List<String> tags,
      boolean visible,
      int sortOrder,
      String createdAt,
      String updatedAt) {
  }

  public record ResumeDraftView(
      ResumeVersion version,
      ResumeBasicInfo basicInfo,
      Map<ResumeSectionType, List<ResumeSectionItem>> sections) {
  }

  public record ResumeUploadTask(
      long id,
      String filename,
      String contentType,
      String status,
      String rawText,
      String parsedJson,
      String errorMessage,
      String createdAt,
      String updatedAt) {
  }

  public record ResumeParsedPayload(
      ResumeBasicInfoRequest basicInfo,
      Map<ResumeSectionType, List<ResumeSectionItemRequest>> sections) {
  }
}
