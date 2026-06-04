package com.mystyle.portfolio.content;

import java.util.List;

public final class ContentModels {
  private ContentModels() {
  }

  public record Profile(
      String name,
      String title,
      String summary,
      String email,
      String education,
      List<String> tags) {
  }

  public record SkillGroup(String category, List<String> items) {
  }

  public record Experience(
      String company,
      String position,
      String period,
      List<String> highlights) {
  }

  public record Evidence(String problem, String solution, String result) {
  }

  public record Project(
      String index,
      String slug,
      String name,
      String summary,
      String role,
      List<String> tech,
      List<String> metrics,
      List<String> responsibilities,
      List<Evidence> evidence) {
  }

  public record ModuleDemo(
      String slug,
      String name,
      String title,
      String demoType,
      String project,
      String summary,
      List<String> tech,
      String apiBase,
      List<String> talkingPoints) {
  }

  public record InterviewGuide(
      String shortIntro,
      List<String> projectOrder,
      List<String> questions,
      List<String> openLinks) {
  }

  public record TimelineItem(
      String type,
      String title,
      String period,
      String summary,
      List<String> tags) {
  }

  public record BlogPost(
      String slug,
      String title,
      String excerpt,
      String content,
      String category,
      List<String> tags,
      String publishedAt,
      int readMinutes) {
  }

  public record HomeView(
      Profile profile,
      List<SkillGroup> skills,
      List<Project> featuredProjects,
      List<ModuleDemo> moduleDemos,
      InterviewGuide interviewGuide) {
  }

  public record ResumeView(
      Profile profile,
      List<SkillGroup> skills,
      List<Experience> experiences,
      List<Project> projects) {
  }
}
