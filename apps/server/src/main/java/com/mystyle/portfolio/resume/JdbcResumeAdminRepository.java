package com.mystyle.portfolio.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.resume.ResumeModels.ResumeBasicInfo;
import com.mystyle.portfolio.resume.ResumeModels.ResumeDraftView;
import com.mystyle.portfolio.resume.ResumeModels.ResumeParsedPayload;
import com.mystyle.portfolio.resume.ResumeModels.ResumeSectionItem;
import com.mystyle.portfolio.resume.ResumeModels.ResumeUploadTask;
import com.mystyle.portfolio.resume.ResumeModels.ResumeVersion;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcResumeAdminRepository implements ResumeAdminRepository {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
  };

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public JdbcResumeAdminRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public ResumeDraftView draft() {
    long draftId = draftVersionId();
    Map<ResumeSectionType, List<ResumeSectionItem>> sections = new EnumMap<>(ResumeSectionType.class);
    for (ResumeSectionType sectionType : ResumeSectionType.values()) {
      sections.put(sectionType, sectionItems(draftId, sectionType));
    }
    return new ResumeDraftView(version(draftId), basicInfo(draftId), sections);
  }

  @Override
  public ResumeDraftView publicResume() {
    long versionId = publishedVersionId();
    Map<ResumeSectionType, List<ResumeSectionItem>> sections = new EnumMap<>(ResumeSectionType.class);
    for (ResumeSectionType sectionType : ResumeSectionType.values()) {
      sections.put(sectionType, visibleSectionItems(versionId, sectionType));
    }
    return new ResumeDraftView(version(versionId), basicInfo(versionId), sections);
  }

  @Override
  public ResumeBasicInfo updateBasicInfo(ResumeBasicInfoRequest request) {
    long draftId = draftVersionId();
    int updatedRows = jdbcTemplate.update(
        """
        UPDATE resume_basic_info
        SET name = ?, title = ?, summary = ?, email = ?, phone = ?, location = ?, education = ?,
            github_url = ?, website_url = ?, updated_at = CURRENT_TIMESTAMP
        WHERE version_id = ?
        """,
        clean(request.name()),
        clean(request.title()),
        clean(request.summary()),
        clean(request.email()),
        clean(request.phone()),
        clean(request.location()),
        clean(request.education()),
        clean(request.githubUrl()),
        clean(request.websiteUrl()),
        draftId);
    if (updatedRows == 0) {
      jdbcTemplate.update(
          """
          INSERT INTO resume_basic_info
            (version_id, name, title, summary, email, phone, location, education, github_url, website_url)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
          draftId,
          clean(request.name()),
          clean(request.title()),
          clean(request.summary()),
          clean(request.email()),
          clean(request.phone()),
          clean(request.location()),
          clean(request.education()),
          clean(request.githubUrl()),
          clean(request.websiteUrl()));
    }
    touchVersion(draftId);
    return basicInfo(draftId);
  }

  @Override
  public List<ResumeSectionItem> sectionItems(ResumeSectionType sectionType) {
    return sectionItems(draftVersionId(), sectionType);
  }

  @Override
  public ResumeSectionItem createSectionItem(ResumeSectionType sectionType, ResumeSectionItemRequest request) {
    long draftId = draftVersionId();
    int sortOrder = request.sortOrder() == null ? nextSortOrder(draftId, sectionType) : request.sortOrder();
    long itemId = insertSectionItem(draftId, sectionType, request, sortOrder);
    touchVersion(draftId);
    return sectionItem(itemId);
  }

  @Override
  public ResumeSectionItem updateSectionItem(long itemId, ResumeSectionItemRequest request) {
    ResumeSectionItem existing = sectionItem(itemId);
    int updatedRows = jdbcTemplate.update(
        """
        UPDATE resume_section_item
        SET title = ?, subtitle = ?, period = ?, summary = ?, detail = ?, tags = ?,
            visible = ?, sort_order = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        clean(request.title()),
        clean(request.subtitle()),
        clean(request.period()),
        clean(request.summary()),
        clean(request.detail()),
        tagsJson(request.tags()),
        visibleValue(request.visible()),
        request.sortOrder() == null ? existing.sortOrder() : request.sortOrder(),
        itemId);
    if (updatedRows == 0) {
      throw ApiException.notFound("履历条目不存在");
    }
    touchVersion(existing.versionId());
    return sectionItem(itemId);
  }

  @Override
  public void deleteSectionItem(long itemId) {
    ResumeSectionItem existing = sectionItem(itemId);
    int deletedRows = jdbcTemplate.update("DELETE FROM resume_section_item WHERE id = ?", itemId);
    if (deletedRows == 0) {
      throw ApiException.notFound("履历条目不存在");
    }
    touchVersion(existing.versionId());
  }

  @Override
  @Transactional
  public ResumeVersion publishDraft() {
    long draftId = draftVersionId();
    jdbcTemplate.update(
        "UPDATE resume_version SET status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP WHERE status = 'PUBLISHED'");
    long publishedId = insertVersion("发布版本 " + DATE_TIME_FORMATTER.format(LocalDateTime.now()), "PUBLISHED", null, LocalDateTime.now());
    copyBasicInfo(draftId, publishedId);
    copyVisibleSectionItems(draftId, publishedId);
    return version(publishedId);
  }

  @Override
  public List<ResumeVersion> versions() {
    return jdbcTemplate.query(
        """
        SELECT id, version_name, status, source_task_id, published_at, created_at, updated_at
        FROM resume_version
        ORDER BY created_at DESC, id DESC
        """,
        (rs, rowNum) -> version(rs));
  }

  @Override
  public ResumeUploadTask createUploadTask(String filename, String contentType, String rawText, String status, String parsedJson, String errorMessage) {
    long taskId = insertUploadTask(filename, contentType, rawText, status, parsedJson, errorMessage);
    return uploadTask(taskId);
  }

  @Override
  public ResumeUploadTask uploadTask(long taskId) {
    return jdbcTemplate.query(
            """
            SELECT id, filename, content_type, status, raw_text, parsed_json, error_message, created_at, updated_at
            FROM resume_upload_task
            WHERE id = ?
            """,
            (rs, rowNum) -> uploadTask(rs),
            taskId)
        .stream()
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("简历解析任务不存在"));
  }

  @Override
  @Transactional
  public ResumeDraftView confirmUploadTask(long taskId) {
    ResumeUploadTask task = uploadTask(taskId);
    if (!"PARSED".equals(task.status()) && !"FALLBACK_REQUIRED".equals(task.status())) {
      throw ApiException.badRequest("当前解析任务不可确认写入");
    }
    if (task.parsedJson() == null || task.parsedJson().isBlank()) {
      throw ApiException.badRequest("解析结果为空，无法写入草稿");
    }
    ResumeParsedPayload payload = parsedPayload(task.parsedJson());
    long draftId = draftVersionId();
    if (payload.basicInfo() != null && payload.basicInfo().name() != null && !payload.basicInfo().name().isBlank()) {
      updateBasicInfo(payload.basicInfo());
    }
    jdbcTemplate.update("DELETE FROM resume_section_item WHERE version_id = ?", draftId);
    for (Map.Entry<ResumeSectionType, List<ResumeSectionItemRequest>> entry : payload.sections().entrySet()) {
      int sortOrder = 1;
      for (ResumeSectionItemRequest request : entry.getValue()) {
        insertSectionItem(draftId, entry.getKey(), request, request.sortOrder() == null ? sortOrder : request.sortOrder());
        sortOrder++;
      }
    }
    jdbcTemplate.update(
        "UPDATE resume_upload_task SET status = 'CONFIRMED', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        taskId);
    jdbcTemplate.update(
        "UPDATE resume_version SET source_task_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        taskId,
        draftId);
    return draft();
  }

  private ResumeVersion version(long versionId) {
    return jdbcTemplate.query(
            """
            SELECT id, version_name, status, source_task_id, published_at, created_at, updated_at
            FROM resume_version
            WHERE id = ?
            """,
            (rs, rowNum) -> version(rs),
            versionId)
        .stream()
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("履历版本不存在"));
  }

  private ResumeVersion version(ResultSet rs) throws SQLException {
    Long sourceTaskId = rs.getObject("source_task_id") == null ? null : rs.getLong("source_task_id");
    return new ResumeVersion(
        rs.getLong("id"),
        rs.getString("version_name"),
        rs.getString("status"),
        sourceTaskId,
        dateTimeString(rs, "published_at"),
        dateTimeString(rs, "created_at"),
        dateTimeString(rs, "updated_at"));
  }

  private ResumeBasicInfo basicInfo(long versionId) {
    return jdbcTemplate.query(
            """
            SELECT id, version_id, name, title, summary, email, phone, location, education, github_url, website_url, updated_at
            FROM resume_basic_info
            WHERE version_id = ?
            """,
            (rs, rowNum) -> basicInfo(rs),
            versionId)
        .stream()
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("履历基础信息不存在"));
  }

  private ResumeBasicInfo basicInfo(ResultSet rs) throws SQLException {
    return new ResumeBasicInfo(
        rs.getLong("id"),
        rs.getLong("version_id"),
        rs.getString("name"),
        rs.getString("title"),
        rs.getString("summary"),
        rs.getString("email"),
        rs.getString("phone"),
        rs.getString("location"),
        rs.getString("education"),
        rs.getString("github_url"),
        rs.getString("website_url"),
        dateTimeString(rs, "updated_at"));
  }

  private List<ResumeSectionItem> sectionItems(long versionId, ResumeSectionType sectionType) {
    return jdbcTemplate.query(
        """
        SELECT id, version_id, section_type, title, subtitle, period, summary, detail, tags, visible, sort_order, created_at, updated_at
        FROM resume_section_item
        WHERE version_id = ? AND section_type = ?
        ORDER BY sort_order, id
        """,
        (rs, rowNum) -> sectionItem(rs),
        versionId,
        sectionType.name());
  }

  private List<ResumeSectionItem> visibleSectionItems(long versionId, ResumeSectionType sectionType) {
    return jdbcTemplate.query(
        """
        SELECT id, version_id, section_type, title, subtitle, period, summary, detail, tags, visible, sort_order, created_at, updated_at
        FROM resume_section_item
        WHERE version_id = ? AND section_type = ? AND visible = 1
        ORDER BY sort_order, id
        """,
        (rs, rowNum) -> sectionItem(rs),
        versionId,
        sectionType.name());
  }

  private ResumeSectionItem sectionItem(long itemId) {
    return jdbcTemplate.query(
            """
            SELECT id, version_id, section_type, title, subtitle, period, summary, detail, tags, visible, sort_order, created_at, updated_at
            FROM resume_section_item
            WHERE id = ?
            """,
            (rs, rowNum) -> sectionItem(rs),
            itemId)
        .stream()
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("履历条目不存在"));
  }

  private ResumeSectionItem sectionItem(ResultSet rs) throws SQLException {
    return new ResumeSectionItem(
        rs.getLong("id"),
        rs.getLong("version_id"),
        ResumeSectionType.from(rs.getString("section_type")),
        rs.getString("title"),
        rs.getString("subtitle"),
        rs.getString("period"),
        rs.getString("summary"),
        rs.getString("detail"),
        tags(rs.getString("tags")),
        rs.getInt("visible") == 1,
        rs.getInt("sort_order"),
        dateTimeString(rs, "created_at"),
        dateTimeString(rs, "updated_at"));
  }

  private ResumeUploadTask uploadTask(ResultSet rs) throws SQLException {
    return new ResumeUploadTask(
        rs.getLong("id"),
        rs.getString("filename"),
        rs.getString("content_type"),
        rs.getString("status"),
        rs.getString("raw_text"),
        rs.getString("parsed_json"),
        rs.getString("error_message"),
        dateTimeString(rs, "created_at"),
        dateTimeString(rs, "updated_at"));
  }

  private ResumeParsedPayload parsedPayload(String parsedJson) {
    try {
      return objectMapper.readValue(parsedJson, ResumeParsedPayload.class);
    } catch (JsonProcessingException exception) {
      throw ApiException.badRequest("解析结果格式错误");
    }
  }

  private long draftVersionId() {
    return jdbcTemplate.query(
            "SELECT id FROM resume_version WHERE status = 'DRAFT' ORDER BY id LIMIT 1",
            (rs, rowNum) -> rs.getLong("id"))
        .stream()
        .findFirst()
        .orElseGet(() -> createDefaultDraft());
  }

  private long publishedVersionId() {
    return jdbcTemplate.query(
            "SELECT id FROM resume_version WHERE status = 'PUBLISHED' ORDER BY published_at DESC, id DESC LIMIT 1",
            (rs, rowNum) -> rs.getLong("id"))
        .stream()
        .findFirst()
        .orElseGet(this::draftVersionId);
  }

  private long createDefaultDraft() {
    long draftId = insertVersion("默认草稿", "DRAFT", null, null);
    jdbcTemplate.update(
        """
        INSERT INTO resume_basic_info
          (version_id, name, title, summary, email, phone, location, education, github_url, website_url)
        VALUES (?, '未填写', '未填写', '待补充个人介绍', 'placeholder@example.com', '', '', '待补充教育经历', '', '')
        """,
        draftId);
    return draftId;
  }

  private long insertVersion(String versionName, String status, Long sourceTaskId, LocalDateTime publishedAt) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement statement = connection.prepareStatement(
          """
          INSERT INTO resume_version (version_name, status, source_task_id, published_at)
          VALUES (?, ?, ?, ?)
          """,
          Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, versionName);
      statement.setString(2, status);
      if (sourceTaskId == null) {
        statement.setObject(3, null);
      } else {
        statement.setLong(3, sourceTaskId);
      }
      if (publishedAt == null) {
        statement.setObject(4, null);
      } else {
        statement.setTimestamp(4, Timestamp.valueOf(publishedAt));
      }
      return statement;
    }, keyHolder);
    return generatedId(keyHolder);
  }

  private long insertSectionItem(long versionId, ResumeSectionType sectionType, ResumeSectionItemRequest request, int sortOrder) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement statement = connection.prepareStatement(
          """
          INSERT INTO resume_section_item
            (version_id, section_type, title, subtitle, period, summary, detail, tags, visible, sort_order)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
          Statement.RETURN_GENERATED_KEYS);
      statement.setLong(1, versionId);
      statement.setString(2, sectionType.name());
      statement.setString(3, clean(request.title()));
      statement.setString(4, clean(request.subtitle()));
      statement.setString(5, clean(request.period()));
      statement.setString(6, clean(request.summary()));
      statement.setString(7, clean(request.detail()));
      statement.setString(8, tagsJson(request.tags()));
      statement.setInt(9, visibleValue(request.visible()));
      statement.setInt(10, sortOrder);
      return statement;
    }, keyHolder);
    return generatedId(keyHolder);
  }

  private long insertUploadTask(String filename, String contentType, String rawText, String status, String parsedJson, String errorMessage) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement statement = connection.prepareStatement(
          """
          INSERT INTO resume_upload_task
            (filename, content_type, status, raw_text, parsed_json, error_message)
          VALUES (?, ?, ?, ?, ?, ?)
          """,
          Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, filename);
      statement.setString(2, contentType);
      statement.setString(3, status);
      statement.setString(4, rawText);
      statement.setString(5, parsedJson);
      statement.setString(6, errorMessage);
      return statement;
    }, keyHolder);
    return generatedId(keyHolder);
  }

  private long generatedId(KeyHolder keyHolder) {
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number id) {
      return id.longValue();
    }
    if (keyHolder.getKey() != null) {
      return keyHolder.getKey().longValue();
    }
    throw ApiException.badRequest("数据库主键生成失败");
  }

  private void copyBasicInfo(long fromVersionId, long toVersionId) {
    ResumeBasicInfo info = basicInfo(fromVersionId);
    jdbcTemplate.update(
        """
        INSERT INTO resume_basic_info
          (version_id, name, title, summary, email, phone, location, education, github_url, website_url)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        toVersionId,
        info.name(),
        info.title(),
        info.summary(),
        info.email(),
        info.phone(),
        info.location(),
        info.education(),
        info.githubUrl(),
        info.websiteUrl());
  }

  private void copyVisibleSectionItems(long fromVersionId, long toVersionId) {
    jdbcTemplate.update(
        """
        INSERT INTO resume_section_item
          (version_id, section_type, title, subtitle, period, summary, detail, tags, visible, sort_order)
        SELECT ?, section_type, title, subtitle, period, summary, detail, tags, visible, sort_order
        FROM resume_section_item
        WHERE version_id = ? AND visible = 1
        ORDER BY sort_order, id
        """,
        toVersionId,
        fromVersionId);
  }

  private int nextSortOrder(long versionId, ResumeSectionType sectionType) {
    Integer max = jdbcTemplate.queryForObject(
        "SELECT COALESCE(MAX(sort_order), 0) FROM resume_section_item WHERE version_id = ? AND section_type = ?",
        Integer.class,
        versionId,
        sectionType.name());
    return (max == null ? 0 : max) + 1;
  }

  private void touchVersion(long versionId) {
    jdbcTemplate.update("UPDATE resume_version SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", versionId);
  }

  private String tagsJson(List<String> tags) {
    try {
      return objectMapper.writeValueAsString(tags == null ? List.of() : tags.stream()
          .filter(tag -> tag != null && !tag.isBlank())
          .map(String::trim)
          .toList());
    } catch (JsonProcessingException exception) {
      throw ApiException.badRequest("标签格式错误");
    }
  }

  private List<String> tags(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(value, STRING_LIST_TYPE);
    } catch (JsonProcessingException exception) {
      return Arrays.stream(value.split(","))
          .map(String::trim)
          .filter(tag -> !tag.isBlank())
          .toList();
    }
  }

  private int visibleValue(Boolean visible) {
    return visible == null || visible ? 1 : 0;
  }

  private String clean(String value) {
    return value == null ? "" : value.trim();
  }

  private String dateTimeString(ResultSet rs, String column) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(column);
    return timestamp == null ? null : DATE_TIME_FORMATTER.format(timestamp.toLocalDateTime());
  }
}
