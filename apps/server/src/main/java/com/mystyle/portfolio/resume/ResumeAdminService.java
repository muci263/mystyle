package com.mystyle.portfolio.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.llm.LlmService;
import com.mystyle.portfolio.resume.ResumeModels.ResumeBasicInfo;
import com.mystyle.portfolio.resume.ResumeModels.ResumeDraftView;
import com.mystyle.portfolio.resume.ResumeModels.ResumeExtractedText;
import com.mystyle.portfolio.resume.ResumeModels.ResumeParsedPayload;
import com.mystyle.portfolio.resume.ResumeModels.ResumeSectionItem;
import com.mystyle.portfolio.resume.ResumeModels.ResumeUploadTask;
import com.mystyle.portfolio.resume.ResumeModels.ResumeVersion;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

@Service
public class ResumeAdminService {
  private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
  private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");
  private static final int MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

  private final ResumeAdminRepository repository;
  private final ObjectMapper objectMapper;
  private final LlmService llmService;

  public ResumeAdminService(ResumeAdminRepository repository, ObjectMapper objectMapper, LlmService llmService) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.llmService = llmService;
  }

  public ResumeDraftView draft() {
    return repository.draft();
  }

  public ResumeDraftView publicResume() {
    return repository.publicResume();
  }

  public ResumeBasicInfo updateBasicInfo(ResumeBasicInfoRequest request) {
    return repository.updateBasicInfo(request);
  }

  public List<ResumeSectionItem> sectionItems(String sectionType) {
    return repository.sectionItems(ResumeSectionType.from(sectionType));
  }

  public ResumeSectionItem createSectionItem(String sectionType, ResumeSectionItemRequest request) {
    return repository.createSectionItem(ResumeSectionType.from(sectionType), request);
  }

  public ResumeSectionItem updateSectionItem(long itemId, ResumeSectionItemRequest request) {
    return repository.updateSectionItem(itemId, request);
  }

  public void deleteSectionItem(long itemId) {
    repository.deleteSectionItem(itemId);
  }

  public ResumeVersion publishDraft() {
    return repository.publishDraft();
  }

  public List<ResumeVersion> versions() {
    return repository.versions();
  }

  public ResumeUploadTask parseUpload(ResumeUploadParseRequest request) {
    String rawText = request.content() == null ? "" : request.content().trim();
    if (rawText.isBlank()) {
      return repository.createUploadTask(
          request.filename(),
          contentType(request.contentType()),
          "",
          "FAILED",
          null,
          "上传内容为空，无法解析");
    }

    boolean allowFallback = Boolean.TRUE.equals(request.allowFallback());
    ResumeParsedPayload payload = allowFallback ? parseRawText(rawText) : llmService.parseResumeText(rawText);
    String parsedJson = toJson(payload);
    boolean hasStructuredSections = payload.sections().values().stream().anyMatch(items -> !items.isEmpty());
    if (!hasStructuredSections) {
      throw ApiException.upstream("没有返回可写入的简历板块；本次不会生成替代任务。");
    }
    return repository.createUploadTask(
        request.filename(),
        contentType(request.contentType()),
        rawText,
        "PARSED",
        parsedJson,
        allowFallback ? "已生成可确认的结构化草稿，请确认后写入草稿" : "Minimax 已完成真实结构化扫描，请确认后写入草稿");
  }

  public ResumeExtractedText extractUploadText(ResumeFileExtractRequest request) {
    byte[] bytes = decodeUpload(request.contentBase64());
    if (bytes.length > MAX_UPLOAD_BYTES) {
      throw ApiException.badRequest("简历文件不能超过 5MB。");
    }
    String filename = request.filename() == null ? "" : request.filename().trim();
    String contentType = contentType(request.contentType());
    String rawText = extractText(filename, contentType, bytes).trim();
    if (rawText.isBlank()) {
      throw ApiException.badRequest("没有从文件中提取到可用文本，请确认 PDF/Word 不是扫描图片或加密文件。");
    }
    return new ResumeExtractedText(filename, contentType, normalizeText(rawText), rawText.length());
  }

  public ResumeDraftView confirmUploadTask(long taskId) {
    return repository.confirmUploadTask(taskId);
  }

  public ResumeUploadTask uploadTask(long taskId) {
    return repository.uploadTask(taskId);
  }

  private byte[] decodeUpload(String contentBase64) {
    try {
      return Base64.getDecoder().decode(contentBase64);
    } catch (IllegalArgumentException exception) {
      throw ApiException.badRequest("文件内容不是合法 Base64。");
    }
  }

  private String extractText(String filename, String contentType, byte[] bytes) {
    String extension = extensionOf(filename);
    try {
      if ("pdf".equals(extension) || contentType.contains("pdf")) {
        return extractPdf(bytes);
      }
      if ("docx".equals(extension) || contentType.contains("wordprocessingml")) {
        return extractDocx(bytes);
      }
      if ("doc".equals(extension) || contentType.equals("application/msword")) {
        return extractDoc(bytes);
      }
      if (Set.of("txt", "md", "markdown").contains(extension) || contentType.startsWith("text/")) {
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
      }
    } catch (ApiException exception) {
      throw exception;
    } catch (IOException | RuntimeException exception) {
      throw ApiException.badRequest("简历文件解析失败：" + trimTo(exception.getMessage(), 160));
    }
    throw ApiException.badRequest("暂不支持该简历格式，请上传 PDF、DOC、DOCX、TXT 或 Markdown。");
  }

  private String extractPdf(byte[] bytes) throws IOException {
    try (PDDocument document = PDDocument.load(bytes)) {
      if (document.isEncrypted()) {
        throw ApiException.badRequest("PDF 文件已加密，无法提取文本。");
      }
      return new PDFTextStripper().getText(document);
    }
  }

  private String extractDocx(byte[] bytes) throws IOException {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
         XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
      return extractor.getText();
    }
  }

  private String extractDoc(byte[] bytes) throws IOException {
    try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
         WordExtractor extractor = new WordExtractor(document)) {
      return extractor.getText();
    }
  }

  private String extensionOf(String filename) {
    int dot = filename == null ? -1 : filename.lastIndexOf('.');
    return dot >= 0 && dot < filename.length() - 1
        ? filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT)
        : "";
  }

  private String normalizeText(String rawText) {
    return rawText
        .replace('\u00A0', ' ')
        .replaceAll("[\\t\\x0B\\f\\r]+", " ")
        .replaceAll(" *\\n *", "\n")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
  }

  private ResumeParsedPayload parseRawText(String rawText) {
    List<String> lines = rawText.lines()
        .map(String::trim)
        .filter(line -> !line.isBlank())
        .toList();

    Map<ResumeSectionType, List<String>> buckets = new EnumMap<>(ResumeSectionType.class);
    for (ResumeSectionType type : ResumeSectionType.values()) {
      buckets.put(type, new ArrayList<>());
    }

    ResumeSectionType current = null;
    for (String line : lines) {
      ResumeSectionType detected = detectSection(line);
      if (detected != null) {
        current = detected;
        continue;
      }
      if (current != null) {
        buckets.get(current).add(line);
      }
    }

    Map<ResumeSectionType, List<ResumeSectionItemRequest>> sections = new EnumMap<>(ResumeSectionType.class);
    for (ResumeSectionType type : ResumeSectionType.values()) {
      sections.put(type, sectionRequests(type, buckets.get(type)));
    }

    if (sections.values().stream().allMatch(List::isEmpty)) {
      sections.put(ResumeSectionType.ADVANTAGE, List.of(new ResumeSectionItemRequest(
          "待人工整理的简历原文",
          "用户确认规则解析",
          "",
          "系统未识别出明确板块，保留原文等待人工确认。",
          rawText,
          List.of("Manual Review"),
          true,
          1)));
    }

    return new ResumeParsedPayload(basicInfoFrom(rawText, lines), sections);
  }

  private List<ResumeSectionItemRequest> sectionRequests(ResumeSectionType type, List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return List.of();
    }
    List<List<String>> groups = splitGroups(lines);
    List<ResumeSectionItemRequest> requests = new ArrayList<>();
    int sortOrder = 1;
    for (List<String> group : groups) {
      if (group.isEmpty()) {
        continue;
      }
      String title = normalizeTitle(group.getFirst(), type);
      String detail = String.join("\n", group);
      requests.add(new ResumeSectionItemRequest(
          title,
          sectionSubtitle(type),
          periodFrom(group),
          summaryFrom(type, group),
          detail,
          tagsFrom(type, group),
          true,
          sortOrder++));
    }
    return requests;
  }

  private List<List<String>> splitGroups(List<String> lines) {
    List<List<String>> groups = new ArrayList<>();
    List<String> current = new ArrayList<>();
    for (String line : lines) {
      if (!current.isEmpty() && looksLikeNewItem(line)) {
        groups.add(current);
        current = new ArrayList<>();
      }
      current.add(line);
    }
    if (!current.isEmpty()) {
      groups.add(current);
    }
    return groups;
  }

  private ResumeBasicInfoRequest basicInfoFrom(String rawText, List<String> lines) {
    String name = lines.isEmpty() ? "待确认" : trimTo(lines.getFirst(), 64);
    String title = rawText.contains("Java") ? "Java 后端开发" : "待确认求职方向";
    String email = find(EMAIL_PATTERN, rawText, "");
    String phone = find(PHONE_PATTERN, rawText, "");
    String education = rawText.contains("山西大学") ? "山西大学 软件工程 本科" : "待确认教育经历";
    return new ResumeBasicInfoRequest(
        name,
        title,
        "由用户确认的规则解析生成，请人工确认后发布。",
        email,
        phone,
        "",
        education,
        "",
        "");
  }

  private ResumeSectionType detectSection(String line) {
    String normalized = line.replace("：", "").replace(":", "").replace(" ", "");
    Map<String, ResumeSectionType> keywords = new LinkedHashMap<>();
    keywords.put("技术能力", ResumeSectionType.SKILL);
    keywords.put("专业技能", ResumeSectionType.SKILL);
    keywords.put("技能", ResumeSectionType.SKILL);
    keywords.put("获奖经历", ResumeSectionType.AWARD);
    keywords.put("奖项", ResumeSectionType.AWARD);
    keywords.put("荣誉", ResumeSectionType.AWARD);
    keywords.put("实习经历", ResumeSectionType.INTERNSHIP);
    keywords.put("工作经历", ResumeSectionType.INTERNSHIP);
    keywords.put("项目经历", ResumeSectionType.PROJECT);
    keywords.put("项目经验", ResumeSectionType.PROJECT);
    keywords.put("个人优势", ResumeSectionType.ADVANTAGE);
    keywords.put("自我评价", ResumeSectionType.ADVANTAGE);
    for (Map.Entry<String, ResumeSectionType> entry : keywords.entrySet()) {
      if (normalized.contains(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  private boolean looksLikeNewItem(String line) {
    return line.matches("^[0-9一二三四五六七八九十]+[.、].+")
        || line.startsWith("项目")
        || line.startsWith("公司")
        || line.contains("有限公司")
        || line.contains("系统")
        || line.contains("平台");
  }

  private String normalizeTitle(String value, ResumeSectionType type) {
    String title = value
        .replaceFirst("^[0-9一二三四五六七八九十]+[.、]\\s*", "")
        .trim();
    if (title.length() > 80 && title.contains("，")) {
      title = title.substring(0, title.indexOf("，"));
    }
    if (title.length() > 80 && title.contains(",")) {
      title = title.substring(0, title.indexOf(","));
    }
    if (title.isBlank()) {
      return sectionSubtitle(type);
    }
    return trimTo(title, 160);
  }

  private String sectionSubtitle(ResumeSectionType type) {
    return switch (type) {
      case SKILL -> "技术能力";
      case AWARD -> "获奖经历";
      case INTERNSHIP -> "实习经历";
      case PROJECT -> "项目经历";
      case ADVANTAGE -> "个人优势";
    };
  }

  private String summaryFrom(ResumeSectionType type, List<String> group) {
    if (group.size() > 1) {
      return trimTo(group.get(1), 220);
    }
    return sectionSubtitle(type);
  }

  private String periodFrom(List<String> group) {
    return group.stream()
        .filter(line -> line.matches(".*20\\d{2}.*"))
        .findFirst()
        .map(line -> trimTo(line, 64))
        .orElse("");
  }

  private List<String> tagsFrom(ResumeSectionType type, List<String> group) {
    String joined = String.join(" ", group);
    List<String> candidates = List.of("Java", "Spring Boot", "Redis", "MySQL", "Docker", "RAG", "Agent", "AI", "Jenkins");
    List<String> tags = candidates.stream()
        .filter(joined::contains)
        .toList();
    if (!tags.isEmpty()) {
      return tags;
    }
    return List.of(sectionSubtitle(type));
  }

  private String find(Pattern pattern, String value, String defaultValue) {
    Matcher matcher = pattern.matcher(value);
    return matcher.find() ? matcher.group() : defaultValue;
  }

  private String trimTo(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }

  private String contentType(String contentType) {
    return contentType == null || contentType.isBlank() ? "text/plain" : contentType.trim();
  }

  private String toJson(ResumeParsedPayload payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw ApiException.badRequest("简历解析结果序列化失败");
    }
  }
}
