package com.mystyle.portfolio.content;

import com.mystyle.portfolio.blog.BlogAnnotationRequest;
import com.mystyle.portfolio.blog.BlogCategoryType;
import com.mystyle.portfolio.blog.BlogCommentRequest;
import com.mystyle.portfolio.blog.BlogPostRequest;
import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.content.ContentModels.BlogAnnotation;
import com.mystyle.portfolio.content.ContentModels.BlogCategory;
import com.mystyle.portfolio.content.ContentModels.BlogComment;
import com.mystyle.portfolio.content.ContentModels.BlogInteractionSummary;
import com.mystyle.portfolio.content.ContentModels.BlogPost;
import com.mystyle.portfolio.content.ContentModels.Evidence;
import com.mystyle.portfolio.content.ContentModels.Experience;
import com.mystyle.portfolio.content.ContentModels.InterviewGuide;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphEdge;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphView;
import com.mystyle.portfolio.content.ContentModels.ModuleDemo;
import com.mystyle.portfolio.content.ContentModels.Profile;
import com.mystyle.portfolio.content.ContentModels.Project;
import com.mystyle.portfolio.content.ContentModels.SkillGroup;
import com.mystyle.portfolio.content.ContentModels.TimelineItem;
import com.mystyle.portfolio.knowledge.KnowledgeGraphEdgeRequest;
import com.mystyle.portfolio.knowledge.KnowledgeGraphNodeRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPortfolioContentRepository implements PortfolioContentRepository {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final Pattern NON_SLUG_CHAR = Pattern.compile("[^a-z0-9]+");
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

  @Override
  public List<BlogPost> blogPosts() {
    return jdbcTemplate.query(
        """
        SELECT id, slug, title, excerpt, content, category, published_at, read_minutes
        FROM blog_post
        WHERE status = 'published'
        ORDER BY sort_order, published_at DESC, id DESC
        """,
        (rs, rowNum) -> blogPost(rs));
  }

  @Override
  public List<BlogCategory> blogCategories() {
    Map<String, Integer> counts = jdbcTemplate.query(
        """
        SELECT category, COUNT(*) AS post_count
        FROM blog_post
        WHERE status = 'published'
        GROUP BY category
        """,
        (rs, rowNum) -> Map.entry(rs.getString("category"), rs.getInt("post_count")))
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return Arrays.stream(BlogCategoryType.values())
        .map(type -> new BlogCategory(type.label(), slugify(type.name()), type.name(), counts.getOrDefault(type.label(), 0)))
        .toList();
  }

  @Override
  public BlogPost createBlogPost(BlogPostRequest request) {
    String slug = uniqueSlug(request.title());
    String category = BlogCategoryType.from(request.category()).label();
    int readMinutes = request.readMinutes() == null || request.readMinutes() <= 0
        ? estimateReadMinutes(request.content())
        : request.readMinutes();
    jdbcTemplate.update(
        """
        INSERT INTO blog_post (slug, title, excerpt, content, category, status, read_minutes, sort_order)
        VALUES (?, ?, ?, ?, ?, 'published', ?, ?)
        """,
        slug,
        request.title().trim(),
        request.excerpt().trim(),
        request.content().trim(),
        category,
        readMinutes,
        nextBlogSortOrder());

    insertTags(blogPostId(slug), request.tags());
    return blogPost(slug);
  }

  @Override
  public BlogPost updateBlogPost(String slug, BlogPostRequest request) {
    long postId = blogPostId(slug);
    String category = BlogCategoryType.from(request.category()).label();
    int readMinutes = request.readMinutes() == null || request.readMinutes() <= 0
        ? estimateReadMinutes(request.content())
        : request.readMinutes();
    jdbcTemplate.update(
        """
        UPDATE blog_post
        SET title = ?, excerpt = ?, content = ?, category = ?, read_minutes = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        request.title().trim(),
        request.excerpt().trim(),
        request.content().trim(),
        category,
        readMinutes,
        postId);
    jdbcTemplate.update("DELETE FROM blog_post_tag WHERE post_id = ?", postId);
    insertTags(postId, request.tags());
    return blogPost(slug);
  }

  @Override
  public List<BlogComment> blogComments(String slug) {
    long postId = blogPostId(slug);
    return jdbcTemplate.query(
        """
        SELECT id, author, content, created_at
        FROM blog_comment
        WHERE post_id = ? AND status = 'visible'
        ORDER BY created_at DESC, id DESC
        """,
        (rs, rowNum) -> blogComment(rs),
        postId);
  }

  @Override
  public BlogComment addBlogComment(String slug, BlogCommentRequest request) {
    long postId = blogPostId(slug);
    jdbcTemplate.update(
        "INSERT INTO blog_comment (post_id, author, content) VALUES (?, ?, ?)",
        postId,
        request.author().trim(),
        request.content().trim());
    return latestBlogComment(postId);
  }

  @Override
  public List<BlogAnnotation> blogAnnotations(String slug) {
    long postId = blogPostId(slug);
    return jdbcTemplate.query(
        """
        SELECT id, anchor_text, note, created_at
        FROM blog_annotation
        WHERE post_id = ?
        ORDER BY created_at DESC, id DESC
        """,
        (rs, rowNum) -> blogAnnotation(rs),
        postId);
  }

  @Override
  public BlogAnnotation addBlogAnnotation(String slug, BlogAnnotationRequest request) {
    long postId = blogPostId(slug);
    jdbcTemplate.update(
        "INSERT INTO blog_annotation (post_id, anchor_text, note) VALUES (?, ?, ?)",
        postId,
        request.anchorText().trim(),
        request.note().trim());
    return latestBlogAnnotation(postId);
  }

  @Override
  public BlogAnnotation updateBlogAnnotation(String slug, long annotationId, BlogAnnotationRequest request) {
    long postId = blogPostId(slug);
    int updatedRows = jdbcTemplate.update(
        """
        UPDATE blog_annotation
        SET anchor_text = ?, note = ?
        WHERE id = ? AND post_id = ?
        """,
        request.anchorText().trim(),
        request.note().trim(),
        annotationId,
        postId);
    if (updatedRows == 0) {
      throw ApiException.notFound("旁注不存在");
    }
    return blogAnnotation(postId, annotationId);
  }

  @Override
  public BlogInteractionSummary deleteBlogAnnotation(String slug, long annotationId) {
    long postId = blogPostId(slug);
    int deletedRows = jdbcTemplate.update(
        "DELETE FROM blog_annotation WHERE id = ? AND post_id = ?",
        annotationId,
        postId);
    if (deletedRows == 0) {
      throw ApiException.notFound("旁注不存在");
    }
    return blogInteractionSummaryById(postId);
  }

  @Override
  public BlogInteractionSummary blogInteractionSummary(String slug) {
    return blogInteractionSummaryById(blogPostId(slug));
  }

  @Override
  public BlogInteractionSummary likeBlogPost(String slug) {
    long postId = blogPostId(slug);
    jdbcTemplate.update(
        "INSERT INTO blog_like (post_id, client_key) VALUES (?, ?)",
        postId,
        "web-" + System.currentTimeMillis());
    return blogInteractionSummaryById(postId);
  }

  @Override
  public KnowledgeGraphView knowledgeGraph() {
    List<KnowledgeGraphNode> nodes = knowledgeGraphNodes(false);
    List<String> visibleNodeKeys = nodes.stream().map(KnowledgeGraphNode::nodeKey).toList();
    List<KnowledgeGraphEdge> edges = knowledgeGraphEdges(true).stream()
        .filter(edge -> visibleNodeKeys.contains(edge.fromNodeKey()) && visibleNodeKeys.contains(edge.toNodeKey()))
        .toList();
    return new KnowledgeGraphView(nodes, edges);
  }

  @Override
  public List<KnowledgeGraphNode> knowledgeGraphNodes(boolean includeHidden) {
    String visibilityClause = includeHidden ? "" : "WHERE visible = 1";
    return jdbcTemplate.query(
        """
        SELECT node_key, label, node_type, level, summary, content, tags, href, source_type, source_slug,
               x, y, z, visible, sort_order
        FROM knowledge_graph_node
        %s
        ORDER BY level, sort_order, id
        """.formatted(visibilityClause),
        (rs, rowNum) -> knowledgeGraphNode(rs));
  }

  @Override
  public KnowledgeGraphNode createKnowledgeGraphNode(KnowledgeGraphNodeRequest request) {
    assertUniqueNodeKey(request.nodeKey());
    jdbcTemplate.update(
        """
        INSERT INTO knowledge_graph_node
        (node_key, label, node_type, level, summary, content, tags, href, source_type, source_slug,
         x, y, z, visible, sort_order)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        normalizedNodeKey(request.nodeKey()),
        request.label().trim(),
        request.nodeType().trim().toUpperCase(Locale.ROOT),
        request.level(),
        trimOrEmpty(request.summary()),
        trimOrEmpty(request.content()),
        csv(request.tags()),
        trimOrEmpty(request.href()),
        trimOrDefault(request.sourceType(), "MANUAL").toUpperCase(Locale.ROOT),
        trimOrEmpty(request.sourceSlug()),
        request.x(),
        request.y(),
        request.z(),
        bool(request.visible()),
        request.sortOrder());
    return knowledgeGraphNode(normalizedNodeKey(request.nodeKey()));
  }

  @Override
  public KnowledgeGraphNode updateKnowledgeGraphNode(String nodeKey, KnowledgeGraphNodeRequest request) {
    String currentKey = normalizedNodeKey(nodeKey);
    assertNodeExists(currentKey);
    String nextKey = normalizedNodeKey(request.nodeKey());
    if (!currentKey.equals(nextKey)) {
      assertUniqueNodeKey(nextKey);
    }
    jdbcTemplate.update(
        """
        UPDATE knowledge_graph_node
        SET node_key = ?, label = ?, node_type = ?, level = ?, summary = ?, content = ?, tags = ?,
            href = ?, source_type = ?, source_slug = ?, x = ?, y = ?, z = ?, visible = ?,
            sort_order = ?, updated_at = CURRENT_TIMESTAMP
        WHERE node_key = ?
        """,
        nextKey,
        request.label().trim(),
        request.nodeType().trim().toUpperCase(Locale.ROOT),
        request.level(),
        trimOrEmpty(request.summary()),
        trimOrEmpty(request.content()),
        csv(request.tags()),
        trimOrEmpty(request.href()),
        trimOrDefault(request.sourceType(), "MANUAL").toUpperCase(Locale.ROOT),
        trimOrEmpty(request.sourceSlug()),
        request.x(),
        request.y(),
        request.z(),
        bool(request.visible()),
        request.sortOrder(),
        currentKey);
    if (!currentKey.equals(nextKey)) {
      jdbcTemplate.update("UPDATE knowledge_graph_edge SET from_node_key = ? WHERE from_node_key = ?", nextKey, currentKey);
      jdbcTemplate.update("UPDATE knowledge_graph_edge SET to_node_key = ? WHERE to_node_key = ?", nextKey, currentKey);
    }
    return knowledgeGraphNode(nextKey);
  }

  @Override
  public void deleteKnowledgeGraphNode(String nodeKey) {
    String normalized = normalizedNodeKey(nodeKey);
    assertNodeExists(normalized);
    jdbcTemplate.update("DELETE FROM knowledge_graph_edge WHERE from_node_key = ? OR to_node_key = ?", normalized, normalized);
    jdbcTemplate.update("DELETE FROM knowledge_graph_node WHERE node_key = ?", normalized);
  }

  @Override
  public List<KnowledgeGraphEdge> knowledgeGraphEdges(boolean includeHidden) {
    String visibilityClause = includeHidden ? "" : "WHERE visible = 1";
    return jdbcTemplate.query(
        """
        SELECT id, from_node_key, to_node_key, relation_type, visible, sort_order
        FROM knowledge_graph_edge
        %s
        ORDER BY sort_order, id
        """.formatted(visibilityClause),
        (rs, rowNum) -> knowledgeGraphEdge(rs));
  }

  @Override
  public KnowledgeGraphEdge createKnowledgeGraphEdge(KnowledgeGraphEdgeRequest request) {
    assertNodeExists(request.fromNodeKey());
    assertNodeExists(request.toNodeKey());
    assertUniqueEdge(request);
    jdbcTemplate.update(
        """
        INSERT INTO knowledge_graph_edge (from_node_key, to_node_key, relation_type, visible, sort_order)
        VALUES (?, ?, ?, ?, ?)
        """,
        normalizedNodeKey(request.fromNodeKey()),
        normalizedNodeKey(request.toNodeKey()),
        trimOrDefault(request.relationType(), "RELATED").toUpperCase(Locale.ROOT),
        bool(request.visible()),
        request.sortOrder());
    return latestKnowledgeGraphEdge();
  }

  @Override
  public KnowledgeGraphEdge updateKnowledgeGraphEdge(long edgeId, KnowledgeGraphEdgeRequest request) {
    assertEdgeExists(edgeId);
    assertNodeExists(request.fromNodeKey());
    assertNodeExists(request.toNodeKey());
    assertUniqueEdge(edgeId, request);
    jdbcTemplate.update(
        """
        UPDATE knowledge_graph_edge
        SET from_node_key = ?, to_node_key = ?, relation_type = ?, visible = ?,
            sort_order = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        normalizedNodeKey(request.fromNodeKey()),
        normalizedNodeKey(request.toNodeKey()),
        trimOrDefault(request.relationType(), "RELATED").toUpperCase(Locale.ROOT),
        bool(request.visible()),
        request.sortOrder(),
        edgeId);
    return knowledgeGraphEdge(edgeId);
  }

  @Override
  public void deleteKnowledgeGraphEdge(long edgeId) {
    int rows = jdbcTemplate.update("DELETE FROM knowledge_graph_edge WHERE id = ?", edgeId);
    if (rows == 0) {
      throw ApiException.notFound("图谱关系不存在");
    }
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

  private BlogPost blogPost(ResultSet rs) throws SQLException {
    long postId = rs.getLong("id");
    BlogInteractionSummary summary = blogInteractionSummaryById(postId);
    return new BlogPost(
        rs.getString("slug"),
        rs.getString("title"),
        rs.getString("excerpt"),
        rs.getString("content"),
        rs.getString("category"),
        strings("SELECT tag FROM blog_post_tag WHERE post_id = ? ORDER BY sort_order, id", postId),
        dateString(rs, "published_at"),
        rs.getInt("read_minutes"),
        summary.likeCount(),
        summary.commentCount(),
        summary.annotationCount());
  }

  private BlogComment blogComment(ResultSet rs) throws SQLException {
    return new BlogComment(
        rs.getLong("id"),
        rs.getString("author"),
        rs.getString("content"),
        dateTimeString(rs, "created_at"));
  }

  private BlogAnnotation blogAnnotation(ResultSet rs) throws SQLException {
    return new BlogAnnotation(
        rs.getLong("id"),
        rs.getString("anchor_text"),
        rs.getString("note"),
        dateTimeString(rs, "created_at"));
  }

  private KnowledgeGraphNode knowledgeGraphNode(ResultSet rs) throws SQLException {
    return new KnowledgeGraphNode(
        rs.getString("node_key"),
        rs.getString("label"),
        rs.getString("node_type"),
        rs.getInt("level"),
        rs.getString("summary"),
        rs.getString("content"),
        csvValues(rs.getString("tags")),
        rs.getString("href"),
        rs.getString("source_type"),
        rs.getString("source_slug"),
        rs.getDouble("x"),
        rs.getDouble("y"),
        rs.getDouble("z"),
        rs.getBoolean("visible"),
        rs.getInt("sort_order"));
  }

  private KnowledgeGraphEdge knowledgeGraphEdge(ResultSet rs) throws SQLException {
    return new KnowledgeGraphEdge(
        rs.getLong("id"),
        rs.getString("from_node_key"),
        rs.getString("to_node_key"),
        rs.getString("relation_type"),
        rs.getBoolean("visible"),
        rs.getInt("sort_order"));
  }

  private KnowledgeGraphNode knowledgeGraphNode(String nodeKey) {
    return jdbcTemplate.query(
            """
            SELECT node_key, label, node_type, level, summary, content, tags, href, source_type, source_slug,
                   x, y, z, visible, sort_order
            FROM knowledge_graph_node
            WHERE node_key = ?
            """,
            (rs, rowNum) -> knowledgeGraphNode(rs),
            normalizedNodeKey(nodeKey))
        .stream()
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("图谱节点不存在"));
  }

  private KnowledgeGraphEdge knowledgeGraphEdge(long edgeId) {
    return jdbcTemplate.query(
            """
            SELECT id, from_node_key, to_node_key, relation_type, visible, sort_order
            FROM knowledge_graph_edge
            WHERE id = ?
            """,
            (rs, rowNum) -> knowledgeGraphEdge(rs),
            edgeId)
        .stream()
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("图谱关系不存在"));
  }

  private KnowledgeGraphEdge latestKnowledgeGraphEdge() {
    return jdbcTemplate.queryForObject(
        """
        SELECT id, from_node_key, to_node_key, relation_type, visible, sort_order
        FROM knowledge_graph_edge
        ORDER BY id DESC
        LIMIT 1
        """,
        (rs, rowNum) -> knowledgeGraphEdge(rs));
  }

  private void assertNodeExists(String nodeKey) {
    if (count("SELECT COUNT(*) FROM knowledge_graph_node WHERE node_key = ?", normalizedNodeKey(nodeKey)) == 0) {
      throw ApiException.notFound("图谱节点不存在");
    }
  }

  private void assertEdgeExists(long edgeId) {
    if (count("SELECT COUNT(*) FROM knowledge_graph_edge WHERE id = ?", edgeId) == 0) {
      throw ApiException.notFound("图谱关系不存在");
    }
  }

  private void assertUniqueNodeKey(String nodeKey) {
    if (count("SELECT COUNT(*) FROM knowledge_graph_node WHERE node_key = ?", normalizedNodeKey(nodeKey)) > 0) {
      throw ApiException.badRequest("图谱节点 key 已存在");
    }
  }

  private void assertUniqueEdge(KnowledgeGraphEdgeRequest request) {
    assertUniqueEdge(0, request);
  }

  private void assertUniqueEdge(long currentEdgeId, KnowledgeGraphEdgeRequest request) {
    if (normalizedNodeKey(request.fromNodeKey()).equals(normalizedNodeKey(request.toNodeKey()))) {
      throw ApiException.badRequest("图谱关系不能连接同一个节点");
    }
    int duplicateCount = count(
        """
        SELECT COUNT(*) FROM knowledge_graph_edge
        WHERE from_node_key = ? AND to_node_key = ? AND relation_type = ? AND id <> ?
        """,
        normalizedNodeKey(request.fromNodeKey()),
        normalizedNodeKey(request.toNodeKey()),
        trimOrDefault(request.relationType(), "RELATED").toUpperCase(Locale.ROOT),
        currentEdgeId);
    if (duplicateCount > 0) {
      throw ApiException.badRequest("图谱关系已存在");
    }
  }

  private String dateString(ResultSet rs, String column) throws SQLException {
    java.sql.Date date = rs.getDate(column);
    return date == null ? null : date.toLocalDate().toString();
  }

  private String dateTimeString(ResultSet rs, String column) throws SQLException {
    LocalDateTime dateTime = rs.getTimestamp(column).toLocalDateTime();
    return DATE_TIME_FORMATTER.format(dateTime);
  }

  private BlogPost blogPost(String slug) {
    return jdbcTemplate.query(
            """
            SELECT id, slug, title, excerpt, content, category, published_at, read_minutes
            FROM blog_post
            WHERE slug = ? AND status = 'published'
            """,
            (rs, rowNum) -> blogPost(rs),
            slug)
        .stream()
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("博客文章不存在"));
  }

  private BlogComment latestBlogComment(long postId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT id, author, content, created_at
        FROM blog_comment
        WHERE post_id = ?
        ORDER BY id DESC
        LIMIT 1
        """,
        (rs, rowNum) -> blogComment(rs),
        postId);
  }

  private BlogAnnotation latestBlogAnnotation(long postId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT id, anchor_text, note, created_at
        FROM blog_annotation
        WHERE post_id = ?
        ORDER BY id DESC
        LIMIT 1
        """,
        (rs, rowNum) -> blogAnnotation(rs),
        postId);
  }

  private BlogAnnotation blogAnnotation(long postId, long annotationId) {
    return jdbcTemplate.query(
            """
            SELECT id, anchor_text, note, created_at
            FROM blog_annotation
            WHERE post_id = ? AND id = ?
            """,
            (rs, rowNum) -> blogAnnotation(rs),
            postId,
            annotationId)
        .stream()
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("旁注不存在"));
  }

  private long blogPostId(String slug) {
    return jdbcTemplate.query(
            "SELECT id FROM blog_post WHERE slug = ? AND status = 'published'",
            (rs, rowNum) -> rs.getLong("id"),
            slug)
        .stream()
        .findFirst()
        .orElseThrow(() -> ApiException.notFound("博客文章不存在"));
  }

  private BlogInteractionSummary blogInteractionSummaryById(long postId) {
    return new BlogInteractionSummary(
        count("SELECT COUNT(*) FROM blog_like WHERE post_id = ?", postId),
        count("SELECT COUNT(*) FROM blog_comment WHERE post_id = ? AND status = 'visible'", postId),
        count("SELECT COUNT(*) FROM blog_annotation WHERE post_id = ?", postId));
  }

  private int count(String sql, Object... args) {
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
    return count == null ? 0 : count;
  }

  private int nextBlogSortOrder() {
    Integer max = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(sort_order), 0) FROM blog_post", Integer.class);
    return (max == null ? 0 : max) + 1;
  }

  private void insertTags(long postId, List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return;
    }
    int sortOrder = 1;
    for (String tag : tags) {
      if (tag == null || tag.isBlank()) {
        continue;
      }
      jdbcTemplate.update(
          "INSERT INTO blog_post_tag (post_id, tag, sort_order) VALUES (?, ?, ?)",
          postId,
          tag.trim(),
          sortOrder++);
    }
  }

  private int estimateReadMinutes(String content) {
    int words = content == null ? 0 : content.trim().length();
    return Math.max(1, (int) Math.ceil(words / 450.0));
  }

  private String uniqueSlug(String title) {
    String baseSlug = slugify(title);
    if (baseSlug.isBlank()) {
      baseSlug = "post";
    }
    String slug = baseSlug;
    int suffix = 2;
    while (count("SELECT COUNT(*) FROM blog_post WHERE slug = ?", slug) > 0) {
      slug = baseSlug + "-" + suffix++;
    }
    return slug;
  }

  private String slugify(String value) {
    String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    String slug = NON_SLUG_CHAR.matcher(normalized).replaceAll("-");
    return slug.replaceAll("^-+|-+$", "");
  }

  private List<String> strings(String sql, Object... args) {
    return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), args);
  }

  private String normalizedNodeKey(String value) {
    return trimOrEmpty(value).toLowerCase(Locale.ROOT);
  }

  private String trimOrEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private String trimOrDefault(String value, String defaultValue) {
    String trimmed = trimOrEmpty(value);
    return trimmed.isBlank() ? defaultValue : trimmed;
  }

  private int bool(boolean value) {
    return value ? 1 : 0;
  }

  private String csv(List<String> values) {
    if (values == null || values.isEmpty()) {
      return "";
    }
    return values.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .distinct()
        .collect(Collectors.joining(","));
  }

  private List<String> csvValues(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .toList();
  }
}
