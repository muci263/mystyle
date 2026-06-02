package com.mystyle.portfolio.content;

import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.content.ContentModels.HomeView;
import com.mystyle.portfolio.content.ContentModels.InterviewGuide;
import com.mystyle.portfolio.content.ContentModels.ModuleDemo;
import com.mystyle.portfolio.content.ContentModels.Project;
import com.mystyle.portfolio.content.ContentModels.ResumeView;
import com.mystyle.portfolio.content.ContentModels.TimelineItem;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PortfolioContentService {
  private final PortfolioContentRepository repository;

  public PortfolioContentService(PortfolioContentRepository repository) {
    this.repository = repository;
  }

  public HomeView home() {
    return new HomeView(
        repository.profile(),
        repository.skills(),
        repository.projects(),
        repository.moduleDemos(),
        repository.interviewGuide());
  }

  public ResumeView resume() {
    return new ResumeView(
        repository.profile(),
        repository.skills(),
        repository.experiences(),
        repository.projects());
  }

  public List<TimelineItem> timeline() {
    return repository.timeline();
  }

  public List<Project> projects(String tech) {
    if (tech == null || tech.isBlank()) {
      return repository.projects();
    }
    String normalized = tech.toLowerCase(Locale.ROOT);
    return repository.projects().stream()
        .filter(project -> containsIgnoreCase(project.tech(), normalized))
        .toList();
  }

  public Project project(String slug) {
    return repository.projects().stream()
        .filter(project -> project.slug().equals(slug))
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("项目不存在"));
  }

  public List<ModuleDemo> moduleDemos(String tech) {
    if (tech == null || tech.isBlank()) {
      return repository.moduleDemos();
    }
    String normalized = tech.toLowerCase(Locale.ROOT);
    return repository.moduleDemos().stream()
        .filter(module -> containsIgnoreCase(module.tech(), normalized))
        .toList();
  }

  public ModuleDemo moduleDemo(String slug) {
    return repository.moduleDemos().stream()
        .filter(module -> module.slug().equals(slug))
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("模块不存在"));
  }

  public InterviewGuide interviewGuide() {
    return repository.interviewGuide();
  }

  private boolean containsIgnoreCase(List<String> values, String keyword) {
    return values.stream()
        .map(value -> value.toLowerCase(Locale.ROOT))
        .anyMatch(value -> value.contains(keyword));
  }
}
