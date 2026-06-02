package com.mystyle.portfolio.content;

import com.mystyle.portfolio.content.ContentModels.Evidence;
import com.mystyle.portfolio.content.ContentModels.Experience;
import com.mystyle.portfolio.content.ContentModels.InterviewGuide;
import com.mystyle.portfolio.content.ContentModels.ModuleDemo;
import com.mystyle.portfolio.content.ContentModels.Profile;
import com.mystyle.portfolio.content.ContentModels.Project;
import com.mystyle.portfolio.content.ContentModels.SkillGroup;
import com.mystyle.portfolio.content.ContentModels.TimelineItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPortfolioContentRepository implements PortfolioContentRepository {
  private final JdbcTemplate jdbcTemplate;

  public JdbcPortfolioContentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Profile profile() {
    return jdbcTemplate.queryForObject(
        "SELECT id, name, title, summary, email, education FROM profile ORDER BY id LIMIT 1",
        (rs, rowNum) -> new Profile(
            rs.getString("name"),
            rs.getString("title"),
            rs.getString("summary"),
            rs.getString("email"),
            rs.getString("education"),
            strings("SELECT tag FROM profile_tag WHERE profile_id = ? ORDER BY sort_order, id", rs.getLong("id"))));
  }

  @Override
  public List<SkillGroup> skills() {
    return jdbcTemplate.query(
        "SELECT id, category FROM skill_group ORDER BY sort_order, id",
        (rs, rowNum) -> new SkillGroup(
            rs.getString("category"),
            strings("SELECT name FROM skill_item WHERE group_id = ? ORDER BY sort_order, id", rs.getLong("id"))));
  }

  @Override
  public List<Experience> experiences() {
    return jdbcTemplate.query(
        "SELECT id, company, position, period FROM experience ORDER BY sort_order, id",
        (rs, rowNum) -> new Experience(
            rs.getString("company"),
            rs.getString("position"),
            rs.getString("period"),
            strings("SELECT highlight FROM experience_highlight WHERE experience_id = ? ORDER BY sort_order, id", rs.getLong("id"))));
  }

  @Override
  public List<Project> projects() {
    return jdbcTemplate.query(
        "SELECT id, project_index, slug, name, summary, role FROM project ORDER BY sort_order, id",
        (rs, rowNum) -> project(rs));
  }

  @Override
  public List<ModuleDemo> moduleDemos() {
    return jdbcTemplate.query(
        "SELECT id, slug, name, title, demo_type, project, summary, api_base FROM module_demo ORDER BY sort_order, id",
        (rs, rowNum) -> moduleDemo(rs));
  }

  @Override
  public InterviewGuide interviewGuide() {
    return jdbcTemplate.queryForObject(
        "SELECT id, short_intro FROM interview_guide ORDER BY id LIMIT 1",
        (rs, rowNum) -> new InterviewGuide(
            rs.getString("short_intro"),
            strings("SELECT item_value FROM interview_project_order WHERE guide_id = ? ORDER BY sort_order, id", rs.getLong("id")),
            strings("SELECT item_value FROM interview_question WHERE guide_id = ? ORDER BY sort_order, id", rs.getLong("id")),
            strings("SELECT item_value FROM interview_open_link WHERE guide_id = ? ORDER BY sort_order, id", rs.getLong("id"))));
  }

  @Override
  public List<TimelineItem> timeline() {
    Profile currentProfile = profile();
    List<Experience> currentExperiences = experiences();
    List<Project> currentProjects = projects();
    return List.of(
        new TimelineItem("education", currentProfile.education(), "本科在读", currentProfile.summary(), currentProfile.tags()),
        new TimelineItem("experience", currentExperiences.getFirst().company(), currentExperiences.getFirst().period(),
            currentExperiences.getFirst().position(), List.of("Java", "Spring Boot", "问题排查")),
        new TimelineItem("project", currentProjects.getFirst().name(), "项目实践", currentProjects.getFirst().summary(),
            currentProjects.getFirst().tech()),
        new TimelineItem("project", currentProjects.get(1).name(), "AI 应用接入", currentProjects.get(1).summary(),
            currentProjects.get(1).tech()));
  }

  private Project project(ResultSet rs) throws SQLException {
    long projectId = rs.getLong("id");
    return new Project(
        rs.getString("project_index"),
        rs.getString("slug"),
        rs.getString("name"),
        rs.getString("summary"),
        rs.getString("role"),
        strings("SELECT tech FROM project_tech WHERE project_id = ? ORDER BY sort_order, id", projectId),
        strings("SELECT metric FROM project_metric WHERE project_id = ? ORDER BY sort_order, id", projectId),
        strings("SELECT responsibility FROM project_responsibility WHERE project_id = ? ORDER BY sort_order, id", projectId),
        evidence(projectId));
  }

  private List<Evidence> evidence(long projectId) {
    return jdbcTemplate.query(
        "SELECT problem, solution, result FROM project_evidence WHERE project_id = ? ORDER BY sort_order, id",
        (rs, rowNum) -> new Evidence(rs.getString("problem"), rs.getString("solution"), rs.getString("result")),
        projectId);
  }

  private ModuleDemo moduleDemo(ResultSet rs) throws SQLException {
    long moduleId = rs.getLong("id");
    return new ModuleDemo(
        rs.getString("slug"),
        rs.getString("name"),
        rs.getString("title"),
        rs.getString("demo_type"),
        rs.getString("project"),
        rs.getString("summary"),
        strings("SELECT tech FROM module_demo_tech WHERE module_id = ? ORDER BY sort_order, id", moduleId),
        rs.getString("api_base"),
        strings("SELECT talking_point FROM module_demo_talking_point WHERE module_id = ? ORDER BY sort_order, id", moduleId));
  }

  private List<String> strings(String sql, Object... args) {
    return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), args);
  }
}
