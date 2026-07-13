package com.mystyle.portfolio.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphEdge;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphView;
import com.mystyle.portfolio.content.ContentModels.ModuleDemo;
import com.mystyle.portfolio.content.ContentModels.Project;
import com.mystyle.portfolio.jd.JdAnalysisResponse;
import com.mystyle.portfolio.jd.JdAnalysisResponse.ModuleRecommendation;
import com.mystyle.portfolio.jd.JdAnalysisResponse.ProjectRecommendation;
import com.mystyle.portfolio.llm.MockInterviewResponse.MockInterviewQuestion;
import com.mystyle.portfolio.resume.ResumeBasicInfoRequest;
import com.mystyle.portfolio.resume.ResumeModels.ResumeParsedPayload;
import com.mystyle.portfolio.resume.ResumeSectionItemRequest;
import com.mystyle.portfolio.resume.ResumeSectionType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class LlmService {
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final String apiKey;
  private final String baseUrl;
  private final String textModel;
  private final String visionModel;
  private final LlmBudgetService budgetService;
  private volatile String authFailureMessage = "";

  @Autowired
  public LlmService(
      ObjectMapper objectMapper,
      @Value("${app.llm.minimax.api-key:${MINIMAX_API_KEY:}}") String apiKey,
      @Value("${app.llm.minimax.base-url:${MINIMAX_BASE_URL:https://api.minimaxi.com/v1}}") String baseUrl,
      @Value("${app.llm.minimax.text-model:${MINIMAX_TEXT_MODEL:MiniMax-M2.7}}") String textModel,
      @Value("${app.llm.minimax.vision-model:${MINIMAX_VISION_MODEL:MiniMax-Text-01}}") String visionModel,
      LlmBudgetService budgetService) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.baseUrl = trimTrailingSlash(baseUrl == null ? "https://api.minimaxi.com/v1" : baseUrl.trim());
    this.textModel = textModel == null || textModel.isBlank() ? "MiniMax-M2.7" : textModel.trim();
    this.visionModel = visionModel == null || visionModel.isBlank() ? "MiniMax-Text-01" : visionModel.trim();
    this.budgetService = budgetService;
    this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
  }

  public LlmService(
      ObjectMapper objectMapper,
      String apiKey,
      String baseUrl,
      String textModel,
      String visionModel) {
    this(objectMapper, apiKey, baseUrl, textModel, visionModel, new LlmBudgetService(
        java.time.Clock.system(java.time.ZoneId.of("Asia/Shanghai")),
        true,
        5.0,
        2.1,
        8.4));
  }

  public LlmProviderStatus status() {
    String mode = !configured() ? "missing-api-key" : authFailureMessage.isBlank() ? "minimax" : "auth-error";
    return new LlmProviderStatus(providerName(), configured(), textModel, visionModel, mode, budgetService.status());
  }

  public List<KnowledgeRelationSuggestion> suggestKnowledgeRelations(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph) {
    return suggestKnowledgeRelationsDetailed(node, graph).suggestions();
  }

  public KnowledgeRelationResult suggestKnowledgeRelationsDetailed(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph) {
    return suggestKnowledgeRelationsDetailed(node, graph, RelationTask.FULL);
  }

  public KnowledgeRelationResult suggestKnowledgeParentRelationsDetailed(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph) {
    return suggestKnowledgeRelationsDetailed(node, graph, RelationTask.TERTIARY_PARENT_ONLY);
  }

  public KnowledgeRelationResult suggestKnowledgeOrchestrateRelationsDetailed(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph) {
    return suggestKnowledgeRelationsDetailed(node, graph, RelationTask.TERTIARY_ORCHESTRATE);
  }

  private KnowledgeRelationResult suggestKnowledgeRelationsDetailed(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph,
      RelationTask task) {
    String content = callMinimax(switch (task) {
      case TERTIARY_PARENT_ONLY -> buildKnowledgeParentPrompt(node, graph);
      case TERTIARY_ORCHESTRATE -> buildKnowledgeOrchestratePrompt(node, graph);
      case FULL -> buildKnowledgePrompt(node, graph);
    });
    List<KnowledgeRelationSuggestion> suggestions = parseRelationSuggestions(content);
    List<KnowledgeRelationSuggestion> sanitized = sanitizeSuggestions(node, graph, suggestions);
    if (sanitized.isEmpty()) {
      if (task == RelationTask.TERTIARY_PARENT_ONLY || task == RelationTask.TERTIARY_ORCHESTRATE) {
        return new KnowledgeRelationResult(
            providerName(),
            true,
            true,
            true,
            List.of(),
            List.of("Minimax 模型调用成功，但没有返回需要新增的候选关系。"));
      }
      throw ApiException.upstream("Minimax 已调用，但没有返回符合图谱层级规则的候选关系；本次不创建任何替代关系。");
    }
    return new KnowledgeRelationResult(
        providerName(),
        true,
        true,
        true,
        sanitized,
        List.of("Minimax 模型调用成功，已按图谱能力边界过滤候选关系。"));
  }

  public ResumeOptimizeResponse optimizeResume(ResumeOptimizeRequest request) {
    String role = request.targetRole() == null || request.targetRole().isBlank() ? "Java 后端开发" : request.targetRole().trim();
    List<String> hints = request.assetHints() == null ? List.of() : request.assetHints();
    String resumeText = nullToEmpty(request.resumeText()).trim();
    if (resumeText.isBlank()) {
      throw ApiException.badRequest("请先上传或粘贴简历内容，再进行 JD 优化。");
    }
    if (Boolean.TRUE.equals(request.allowFallback())) {
      return new ResumeOptimizeResponse(
          "local-template",
          role,
          "已生成本地辅助草稿，请结合真实经历人工确认后使用。",
          fallbackSummary(request.resumeText(), role),
          fallbackGeneratedResume(request.resumeText(), request.jdText(), role, hints),
          List.of(
              "优先保留真实项目证据和量化指标。",
              "围绕岗位 JD 重排项目顺序，突出匹配技术栈。",
              hints.isEmpty() ? "补充可验证的实习、项目和模块 Demo 证据。" : "可重点引用：" + String.join("、", hints)),
          List.of("项目经历：按 JD 关键词重排，先讲最匹配项目。", "技术能力：把 JD 高频技术栈前置。"),
          List.of("这是本地模板生成的辅助结果，请人工确认后使用。"));
    }
    String content = callMinimax("""
        你是简历优化助手。只基于用户提供的真实信息给出中文建议，不编造经历。
        只能输出一个严格 JSON 对象，不要 Markdown，不要代码块，不要解释文字，不要 <think>。
        所有数组项必须是字符串，字符串内的双引号必须转义。
        目标是基于上传简历和岗位 JD 生成一份可投递的“JD 适配版简历”。
        生成规则：
        1. generatedResumeMarkdown 必须是完整中文简历 Markdown，包含：姓名/标题摘要、技术能力、项目经历、实习或实践经历、个人优势。
        2. 可以重排、压缩、改写表达，但不得新增上传简历中不存在的公司、学校、项目、奖项、数字指标。
        3. 如果 JD 要求但简历没有证据，写入 riskNotes，不要硬塞到简历正文。
        4. rewrittenSummary 是适合放在简历开头的 1 段候选摘要。
        返回 JSON：{"summary":"...","rewrittenSummary":"...","generatedResumeMarkdown":"# 简历...","highlights":["..."],"sectionSuggestions":["..."],"riskNotes":["..."]}。
        目标岗位：%s
        简历文本：%s
        岗位JD：%s
        资产提示：%s
        """.formatted(role, nullToEmpty(request.resumeText()), nullToEmpty(request.jdText()), String.join("、", hints)));
    JsonNode json = extractJson(content);
    String summary = requiredText(json, "summary", "简历优化 summary");
    String rewrittenSummary = requiredText(json, "rewrittenSummary", "简历优化 rewrittenSummary");
    String generatedResumeMarkdown = requiredText(json, "generatedResumeMarkdown", "简历优化 generatedResumeMarkdown");
    assertNoUnsafeGeneratedText(
        "简历优化 generatedResumeMarkdown",
        generatedResumeMarkdown,
        resumeText + "\n" + nullToEmpty(request.jdText()) + "\n" + String.join("\n", hints));
    return new ResumeOptimizeResponse(
        providerName(),
        role,
        summary,
        rewrittenSummary,
        generatedResumeMarkdown,
        strings(json, "highlights"),
        strings(json, "sectionSuggestions"),
        strings(json, "riskNotes"));
  }

  public MockInterviewResponse mockInterview(MockInterviewRequest request) {
    String role = request.targetRole() == null || request.targetRole().isBlank() ? "Java 后端开发" : request.targetRole().trim();
    List<String> hints = request.assetHints() == null ? List.of() : request.assetHints();
    String resumeText = nullToEmpty(request.resumeText()).trim();
    if (resumeText.isBlank()) {
      throw ApiException.badRequest("请先上传或生成简历内容，再进行模拟面试。");
    }
    if (Boolean.TRUE.equals(request.allowFallback())) {
      return new MockInterviewResponse(
          "local-template",
          role,
          fallbackInterviewQuestions(role, hints),
          "建议优先围绕真实项目、岗位关键词和风险缺口做 3 轮演练。",
          List.of("这是本地模板生成的辅助结果，请人工确认后使用。"));
    }
    String content = callMinimax("""
        你是严谨的 Java 后端面试官和面试教练。请基于上传简历与 JD 生成模拟面试问答。
        只能输出一个严格 JSON 对象，不要 Markdown，不要代码块，不要解释文字，不要 <think>。
        边界：
        1. 只能基于简历、JD、资产提示发问和作答，不得编造候选人没有的经历。
        2. strongAnswer 要像候选人口吻，结构为：结论 -> 项目证据 -> 技术细节 -> 结果/反思。
        3. followUps 是面试官可能继续追问的问题；scoreFocus 是回答时要命中的评分点。
        4. 至少 5 题，覆盖：自我介绍、岗位匹配、最强项目、技术深挖、问题排查/优化、AI/RAG 或加分项。
        返回 JSON：
        {
          "questions":[
            {"question":"...","intent":"...","strongAnswer":"...","followUps":["..."],"scoreFocus":["..."]}
          ],
          "closingAdvice":"...",
          "riskNotes":["..."]
        }
        目标岗位：%s
        岗位JD：%s
        简历文本：%s
        资产提示：%s
        """.formatted(role, nullToEmpty(request.jdText()), resumeText, String.join("、", hints)));
    JsonNode json = extractJson(content);
    List<MockInterviewQuestion> questions = interviewQuestions(json.path("questions"));
    if (questions.isEmpty()) {
      throw ApiException.upstream("Minimax 已调用，但没有返回可用的模拟面试问题；本次不生成替代结果。");
    }
    assertInterviewRespectsSources(questions, resumeText + "\n" + nullToEmpty(request.jdText()) + "\n" + String.join("\n", hints));
    return new MockInterviewResponse(
        providerName(),
        role,
        questions,
        requiredText(json, "closingAdvice", "模拟面试 closingAdvice"),
        strings(json, "riskNotes"));
  }

  public InterviewTurnResponse nextInterviewQuestion(InterviewTurnRequest request) {
    String role = request.targetRole() == null || request.targetRole().isBlank() ? "Java 后端开发" : request.targetRole().trim();
    List<String> hints = request.assetHints() == null ? List.of() : request.assetHints();
    List<InterviewAnswer> history = request.history() == null ? List.of() : request.history();
    String resumeText = nullToEmpty(request.resumeText()).trim();
    if (resumeText.isBlank()) {
      throw ApiException.badRequest("请先上传、粘贴或选择简历，再开始模拟面试。");
    }
    if (Boolean.TRUE.equals(request.allowFallback())) {
      MockInterviewQuestion question = fallbackInterviewQuestions(role, hints)
          .get(Math.min(Math.max(history.size(), 0), fallbackInterviewQuestions(role, hints).size() - 1));
      return new InterviewTurnResponse(
          "local-template",
          role,
          history.size() + 1,
          question.question(),
          question.intent(),
          question.followUps(),
          question.scoreFocus(),
          history.size() >= 2,
          List.of("本地辅助问题，仅用于离线演练。"));
    }
    String content = callMinimax("""
        你是严谨的 Java 后端面试官。请生成“下一道”模拟面试问题。
        只能输出一个严格 JSON 对象，不要 Markdown，不要代码块，不要解释文字，不要 <think>。
        输入输出契约：
        1. 只问 1 个问题，不要一次性给多题。
        2. 问题必须基于简历、JD、资产提示和历史回答，不得编造候选人没有的经历。
        3. 避免重复历史问题；每轮逐步推进：自我介绍 -> 项目证据 -> 技术深挖 -> 风险补强 -> 反问/总结。
        4. intent 说明这题考察什么；followUps 给 1-3 个可能追问；scoreFocus 给 2-5 个评分点。
        5. canFinish 表示当前历史足够生成适配简历和反馈，至少回答 2 题后才可为 true。
        返回 JSON：{"question":"...","intent":"...","followUps":["..."],"scoreFocus":["..."],"canFinish":false,"notes":["..."]}。
        目标岗位：%s
        岗位JD：%s
        简历文本：%s
        资产提示：%s
        历史问答：%s
        当前轮次：%s
        """.formatted(
            role,
            nullToEmpty(request.jdText()),
            resumeText,
            String.join("、", hints),
            toJson(history),
            request.round() == null ? history.size() + 1 : request.round()));
    JsonNode json = extractJson(content);
    String question = requiredText(json, "question", "模拟面试 question");
    if (history.stream().map(InterviewAnswer::question).anyMatch(previous -> normalizeCompare(previous).equals(normalizeCompare(question)))) {
      throw ApiException.upstream("Minimax 已调用，但返回了重复面试问题；本次不生成替代问题。");
    }
    return new InterviewTurnResponse(
        providerName(),
        role,
        history.size() + 1,
        question,
        requiredText(json, "intent", "模拟面试 intent"),
        strings(json, "followUps"),
        requiredStrings(json, "scoreFocus", "模拟面试 scoreFocus"),
        history.size() >= 2 && json.path("canFinish").asBoolean(false),
        strings(json, "notes"));
  }

  public InterviewFinalizeResponse finalizeInterview(InterviewFinalizeRequest request) {
    String role = request.targetRole() == null || request.targetRole().isBlank() ? "Java 后端开发" : request.targetRole().trim();
    List<String> hints = request.assetHints() == null ? List.of() : request.assetHints();
    List<InterviewAnswer> history = request.history() == null ? List.of() : request.history();
    String resumeText = nullToEmpty(request.resumeText()).trim();
    if (resumeText.isBlank()) {
      throw ApiException.badRequest("请先提供简历内容，再生成面试结果。");
    }
    if (history.isEmpty()) {
      throw ApiException.badRequest("请至少完成一轮模拟面试问答，再生成结果。");
    }
    if (Boolean.TRUE.equals(request.allowFallback())) {
      String generated = fallbackGeneratedResume(resumeText, request.jdText(), role, hints);
      return new InterviewFinalizeResponse(
          "local-template",
          role,
          fallbackSummary(resumeText, role),
          generated,
          fallbackKeywords(request.jdText(), hints),
          List.of("围绕 JD 关键词重排简历内容。", "保留真实项目证据，不扩写无证据经历。"),
          List.of("回答应继续补充职责边界、量化结果和排障细节。"),
          List.of("人工确认适配简历，再决定是否写入草稿。"),
          List.of("本地辅助结果，请人工确认后使用。"),
          history.size());
    }
    String content = callMinimax("""
        你是候选人的面试复盘与简历优化助手。请基于 JD、原始简历和模拟面试问答生成最终结果包。
        只能输出一个严格 JSON 对象，不要 Markdown 代码块，不要解释文字，不要 <think>。
        输入输出契约：
        1. generatedResumeMarkdown 必须是完整中文 Markdown 简历，可直接投递或保存到草稿。
        2. 只允许重排、压缩、改写已有真实经历；不得新增简历、JD、问答中不存在的公司、项目、奖项、学校或数字指标。
        3. 如果岗位要求缺少证据，写入 riskNotes 和 nextActions，不要硬塞进简历正文。
        4. interviewFeedback 必须基于用户真实回答，指出表达亮点和需要补强的地方。
        返回 JSON：
        {
          "summary":"不超过120字的结果摘要",
          "generatedResumeMarkdown":"# ...",
          "highlights":["..."],
          "resumeSuggestions":["..."],
          "interviewFeedback":["..."],
          "nextActions":["..."],
          "riskNotes":["..."]
        }
        目标岗位：%s
        岗位JD：%s
        原始简历：%s
        资产提示：%s
        模拟面试问答：%s
        """.formatted(
            role,
            nullToEmpty(request.jdText()),
            resumeText,
            String.join("、", hints),
            toJson(history)));
    JsonNode json = extractJson(content);
    String generatedResumeMarkdown = requiredText(json, "generatedResumeMarkdown", "面试结果 generatedResumeMarkdown");
    String source = resumeText + "\n" + nullToEmpty(request.jdText()) + "\n" + String.join("\n", hints) + "\n" + toJson(history);
    assertNoUnsafeGeneratedText("面试结果 generatedResumeMarkdown", generatedResumeMarkdown, source);
    return new InterviewFinalizeResponse(
        providerName(),
        role,
        requiredText(json, "summary", "面试结果 summary"),
        generatedResumeMarkdown,
        requiredStrings(json, "highlights", "面试结果 highlights"),
        requiredStrings(json, "resumeSuggestions", "面试结果 resumeSuggestions"),
        requiredStrings(json, "interviewFeedback", "面试结果 interviewFeedback"),
        requiredStrings(json, "nextActions", "面试结果 nextActions"),
        strings(json, "riskNotes"),
        history.size());
  }

  public String runAgentWorkflow(String question, String templateId) {
    return runAgentWorkflow(question, templateId, "");
  }

  public String runAgentWorkflow(String question, String templateId, String memory) {
    String content = callMinimax(buildAgentWorkflowPrompt(question, memory));
    if (content.isBlank()) {
      throw ApiException.upstream("Minimax Agent Workflow 返回空内容。");
    }
    return plainAgentAnswer(content);
  }

  public String streamAgentWorkflow(String question, String templateId, Consumer<String> onDelta) {
    return streamAgentWorkflow(question, templateId, "", onDelta);
  }

  public String streamAgentWorkflow(String question, String templateId, String memory, Consumer<String> onDelta) {
    String content = callMinimaxStream(buildAgentWorkflowPrompt(question, memory), onDelta);
    if (content.isBlank()) {
      throw ApiException.upstream("Minimax Agent Workflow 流式返回空内容。");
    }
    return plainAgentAnswer(content);
  }

  private String buildAgentWorkflowPrompt(String question, String memory) {
    return """
        你是作品集里的语言助手。请直接回答用户问题，回答应自然、具体、可执行。
        要求：
        1. 不要提及工作流模板、rag-question、RAG、工具边界、受约束回答、未接入检索器等实现细节。
        2. 不要声称已经执行真实数据库查询、真实检索或真实外部工具。
        3. 如果问题缺少必要背景，可以直接提出 1 个澄清问题。
        4. 如果会话记忆与当前问题相关，可以自然引用；如果无关，不要强行提及。
        5. 只能输出中文纯文本，80-220 字；不要 JSON，不要 Markdown，不要代码块，不要 <think>。
        会话记忆：
        %s
        用户问题：%s
        """.formatted(trimTo(nullToEmpty(memory), 1800), nullToEmpty(question));
  }

  public ResumeParsedPayload parseResumeText(String rawText) {
    String content = callMinimax("""
        你是中文简历结构化解析器。只抽取简历原文中存在的信息，不编造经历；原文没有的信息必须返回空字符串或空数组。
        只能输出一个严格 JSON 对象，不要 Markdown，不要代码块，不要解释文字，不要 <think>。
        basicInfo.name 必须从原文姓名抽取；basicInfo.education 必须从原文学校/学历抽取。
        basicInfo.title 和 basicInfo.summary 可以基于原文技能、项目、求职方向做一句真实归纳，但不得新增原文外经历。
        返回严格 JSON：
        {
          "basicInfo":{"name":"","title":"","summary":"","email":"","phone":"","location":"","education":"","githubUrl":"","websiteUrl":""},
          "sections":{
            "SKILL":[{"title":"","subtitle":"","period":"","summary":"","detail":"","tags":["Java"],"visible":true,"sortOrder":1}],
            "AWARD":[],
            "INTERNSHIP":[],
            "PROJECT":[],
            "ADVANTAGE":[]
          }
        }
        section 只允许 SKILL, AWARD, INTERNSHIP, PROJECT, ADVANTAGE。
        每个条目 detail 保留关键 bullet，summary 用一句话概括。
        简历原文：
        %s
        """.formatted(nullToEmpty(rawText)));
    JsonNode json = extractJson(content);
    ResumeBasicInfoRequest basicInfo = basicInfoFromJson(json.path("basicInfo"), rawText);
    Map<ResumeSectionType, List<ResumeSectionItemRequest>> sections = new EnumMap<>(ResumeSectionType.class);
    JsonNode sectionRoot = json.path("sections");
    for (ResumeSectionType type : ResumeSectionType.values()) {
      sections.put(type, sectionItemsFromJson(type, sectionRoot.path(type.name())));
    }
    if (sections.values().stream().allMatch(List::isEmpty)) {
      throw ApiException.upstream("Minimax 已调用，但没有返回任何可写入的简历板块；本次不生成替代草稿。");
    }
    return new ResumeParsedPayload(basicInfo, sections);
  }

  public JdAnalysisResponse analyzeJd(
      long analysisId,
      String jdText,
      String role,
      Set<String> keywords,
      int matchScore,
      List<Project> projects,
      List<ModuleDemo> modules) {
    String content = callMinimax("""
        你是 Java 后端秋招作品集 JD 适配助手。只能基于已有资产生成建议，不编造项目。
        只能输出一个严格 JSON 对象，不要 Markdown，不要代码块，不要解释文字，不要 <think>。
        projectRecommendations 和 moduleRecommendations 必须只使用输入资产中存在的 slug。
        返回 JSON：
        {
          "role":"岗位定位",
          "keywords":["Java"],
          "matchScore":88,
          "summary":"整体适配说明",
          "projectRecommendations":[{"slug":"已有项目slug","emphasis":"怎么讲","supportedBy":["证据"]}],
          "moduleRecommendations":[{"slug":"已有模块slug","reason":"为什么推荐"}],
          "resumeOptimizations":["简历优化建议"],
          "interviewTalkingPoints":["面试讲解点"],
          "riskNotes":["风险"]
        }
        projectRecommendations.slug 只能从项目资产里选择；moduleRecommendations.slug 只能从模块资产里选择。
        JD：%s
        初始岗位：%s
        初始关键词：%s
        项目资产：%s
        模块资产：%s
        """.formatted(nullToEmpty(jdText), role, String.join("、", keywords), toJson(projects), toJson(modules)));
    JsonNode json = extractJson(content);
    Set<String> projectSlugs = new LinkedHashSet<>(projects.stream().map(Project::slug).toList());
    Set<String> moduleSlugs = new LinkedHashSet<>(modules.stream().map(ModuleDemo::slug).toList());
    List<ProjectRecommendation> projectRecommendations = projectRecommendations(json.path("projectRecommendations"), projects, projectSlugs);
    List<ModuleRecommendation> moduleRecommendations = moduleRecommendations(json.path("moduleRecommendations"), modules, moduleSlugs);
    if (projectRecommendations.isEmpty() && moduleRecommendations.isEmpty()) {
      throw ApiException.upstream("Minimax 已调用，但没有返回任何合法的项目或模块推荐；本次不生成替代结果。");
    }
    return new JdAnalysisResponse(
        analysisId,
        providerName(),
        requiredText(json, "role", "JD 分析 role"),
        requiredStrings(json, "keywords", "JD 分析 keywords"),
        clampInt(requiredInt(json, "matchScore", "JD 分析 matchScore"), 0, 100),
        requiredText(json, "summary", "JD 分析 summary"),
        projectRecommendations,
        moduleRecommendations,
        requiredStrings(json, "resumeOptimizations", "JD 分析 resumeOptimizations"),
        requiredStrings(json, "interviewTalkingPoints", "JD 分析 interviewTalkingPoints"),
        strings(json, "riskNotes"));
  }

  public String providerName() {
    return "minimax-chat-completions";
  }

  public boolean configured() {
    return !apiKey.isBlank();
  }

  private String callMinimax(String prompt) {
    ensureConfigured();
    String systemPrompt = "你是作品集知识图谱与简历优化助手。除非用户明确要求自然语言回答，否则输出必须是严格 JSON；不要输出 Markdown 代码块或 <think> 推理文本。";
    int maxTokens = 5000;
    Map<String, Object> body = Map.of(
        "model", textModel,
        "messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", prompt)),
        "temperature", 0.2,
        "max_tokens", maxTokens);
    int estimatedInputTokens = estimateTokens(systemPrompt) + estimateTokens(prompt) + 12;
    LlmBudgetService.Reservation reservation = budgetService.reserve(estimatedInputTokens, maxTokens);
    String response;
    try {
      response = restClient.post()
          .uri("/chat/completions")
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + apiKey)
          .body(body)
          .retrieve()
          .body(String.class);
    } catch (RestClientResponseException exception) {
      reservation.close();
      throw ApiException.upstream("Minimax HTTP 调用失败：" + exception.getStatusCode() + " " + trimTo(exception.getResponseBodyAsString(), 180));
    } catch (RuntimeException exception) {
      reservation.close();
      throw ApiException.upstream("Minimax 网络或客户端调用失败：" + trimTo(exception.getMessage(), 180));
    }
    JsonNode root;
    try {
      root = objectMapper.readTree(response);
    } catch (Exception exception) {
      reservation.close();
      throw ApiException.upstream("Minimax 响应不是合法 JSON：" + trimTo(response, 180));
    }
    JsonNode baseResp = root.path("base_resp");
    if (baseResp.path("status_code").asInt(0) != 0) {
      reservation.close();
      int statusCode = baseResp.path("status_code").asInt(0);
      String statusMessage = baseResp.path("status_msg").asText("unknown");
      String message = "Minimax API error: " + statusMessage;
      if (statusCode == 2049 || statusMessage.toLowerCase(Locale.ROOT).contains("invalid api key")) {
        authFailureMessage = message + "。请在 MiniMax Open Platform 的 Account Management > API Keys 重新生成有效 API Key。";
      }
      throw ApiException.upstream(message);
    }
    JsonNode content = root.path("choices").path(0).path("message").path("content");
    if (!content.isTextual() || content.asText().isBlank()) {
      reservation.close();
      throw ApiException.upstream("Minimax 响应缺少 choices[0].message.content。");
    }
    String stripped = stripReasoning(content.asText());
    TokenUsage usage = tokenUsage(root, estimatedInputTokens, estimateTokens(stripped));
    reservation.complete(usage.inputTokens(), usage.outputTokens());
    return stripped;
  }

  private String callMinimaxStream(String prompt, Consumer<String> onDelta) {
    ensureConfigured();
    String systemPrompt = "你是作品集语言助手。直接回答用户问题，不暴露工作流、RAG、工具边界或系统实现细节。";
    int maxTokens = 1200;
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", textModel);
    body.put("messages", List.of(
        Map.of("role", "system", "content", systemPrompt),
        Map.of("role", "user", "content", prompt)));
    body.put("temperature", 0.2);
    body.put("max_tokens", maxTokens);
    body.put("stream", true);

    int estimatedInputTokens = estimateTokens(systemPrompt) + estimateTokens(prompt) + 12;
    LlmBudgetService.Reservation reservation = budgetService.reserve(estimatedInputTokens, maxTokens);
    StringBuilder answer = new StringBuilder();
    try {
      restClient.post()
          .uri("/chat/completions")
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + apiKey)
          .body(body)
          .exchange((request, response) -> {
            if (response.getStatusCode().isError()) {
              String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
              throw ApiException.upstream("Minimax 流式 HTTP 调用失败：" + response.getStatusCode() + " " + trimTo(responseBody, 180));
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
              String line;
              while ((line = reader.readLine()) != null) {
                String payload = streamPayload(line);
                if (payload.isBlank()) {
                  continue;
                }
                if ("[DONE]".equals(payload)) {
                  break;
                }
                String delta = streamDelta(payload);
                if (delta.isBlank()) {
                  continue;
                }
                String cleaned = stripReasoning(delta);
                if (cleaned.isBlank()) {
                  continue;
                }
                answer.append(cleaned);
                onDelta.accept(cleaned);
              }
            }
            return null;
          });
    } catch (ApiException exception) {
      reservation.close();
      throw exception;
    } catch (RestClientResponseException exception) {
      reservation.close();
      throw ApiException.upstream("Minimax 流式 HTTP 调用失败：" + exception.getStatusCode() + " " + trimTo(exception.getResponseBodyAsString(), 180));
    } catch (RuntimeException exception) {
      reservation.close();
      throw ApiException.upstream("Minimax 流式调用失败：" + trimTo(exception.getMessage(), 180));
    }
    String result = stripReasoning(answer.toString()).trim();
    if (result.isBlank()) {
      reservation.close();
      throw ApiException.upstream("Minimax 流式响应没有返回文本增量。");
    }
    reservation.complete(estimatedInputTokens, estimateTokens(result));
    return result;
  }

  private String streamPayload(String line) {
    String trimmed = line == null ? "" : line.trim();
    if (trimmed.isBlank() || trimmed.startsWith(":")) {
      return "";
    }
    if (trimmed.startsWith("data:")) {
      return trimmed.substring("data:".length()).trim();
    }
    return trimmed;
  }

  private String streamDelta(String payload) {
    try {
      JsonNode root = objectMapper.readTree(payload);
      JsonNode baseResp = root.path("base_resp");
      if (!baseResp.isMissingNode() && baseResp.path("status_code").asInt(0) != 0) {
        int statusCode = baseResp.path("status_code").asInt(0);
        String statusMessage = baseResp.path("status_msg").asText("unknown");
        String message = "Minimax API error: " + statusMessage;
        if (statusCode == 2049 || statusMessage.toLowerCase(Locale.ROOT).contains("invalid api key")) {
          authFailureMessage = message + "。请在 MiniMax Open Platform 的 Account Management > API Keys 重新生成有效 API Key。";
        }
        throw ApiException.upstream(message);
      }
      JsonNode choice = root.path("choices").path(0);
      JsonNode delta = choice.path("delta").path("content");
      if (delta.isTextual()) {
        return delta.asText();
      }
      JsonNode message = choice.path("message").path("content");
      return message.isTextual() ? message.asText() : "";
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      throw ApiException.upstream("Minimax 流式响应片段无法解析：" + trimTo(payload, 160));
    }
  }

  private String buildKnowledgePrompt(KnowledgeGraphNode node, KnowledgeGraphView graph) {
    List<Map<String, Object>> existingNodes = graph.nodes().stream()
        .map(item -> Map.<String, Object>of(
            "nodeKey", item.nodeKey(),
            "label", item.label(),
            "nodeType", item.nodeType(),
            "level", item.level(),
            "summary", item.summary(),
            "tags", item.tags()))
        .toList();
    return """
        请为当前知识图谱节点生成 1-3 条“必要关系”，只能连接到 existingNodes 中存在的节点。
        能力边界：
        1. 只表达作品集导航、项目证据、技术能力和博客解释之间的真实关系，不要为了图谱好看而造边。
        2. 每条关系必须直接包含当前节点，不要替其他两个节点新建关系。
        3. 层级契约：level 0 是一级个人核心，只能 CORE->SECTION；level 1 是二级栏目，只能 SECTION->level2；level 2 是三级内容节点，只能和所属二级栏目连接，或与其他 level2 节点建立证据关系。
        4. 禁止跨级连接，例如 level2 不能直接连 level0；禁止反向指向 CORE；禁止 SECTION 之间互连。
        5. 优先生成 1 条归属边：CORE->SECTION 用 OWNS；section-blog->BLOG 用 CONTAINS；section-evidence->PROJECT/MODULE 用 INCLUDES；section-resume->SKILL 用 INCLUDES。
        6. 证据边最多 2 条：PROJECT/MODULE->SKILL 用 USES；BLOG->SKILL/PROJECT/MODULE 用 EXPLAINS；只有标签和摘要明确重合时才用 RELATED。
        关系类型只能使用 OWNS, INCLUDES, CONTAINS, USES, EXPLAINS, RELATED。
        输出契约：返回严格 JSON 数组，不要 Markdown，不要解释文字，不要 <think>。
        JSON schema: [{"fromNodeKey":"existing node key","toNodeKey":"existing node key","relationType":"OWNS|INCLUDES|CONTAINS|USES|EXPLAINS|RELATED","reason":"不超过60字的依据"}]。
        新节点：%s
        已有节点：%s
        """.formatted(toJson(node), toJson(existingNodes));
  }

  private String buildKnowledgeParentPrompt(KnowledgeGraphNode node, KnowledgeGraphView graph) {
    Map<String, KnowledgeGraphNode> nodeByKey = new LinkedHashMap<>();
    graph.nodes().forEach(item -> nodeByKey.put(item.nodeKey(), item));
    List<Map<String, Object>> sectionNodes = graph.nodes().stream()
        .filter(item -> item.level() == 1)
        .map(item -> Map.<String, Object>of(
            "nodeKey", item.nodeKey(),
            "label", item.label(),
            "nodeType", item.nodeType(),
            "level", item.level(),
            "summary", item.summary(),
            "tags", item.tags()))
        .toList();
    List<Map<String, Object>> existingParentEdges = graph.edges().stream()
        .filter(edge -> edge.toNodeKey().equals(node.nodeKey()))
        .filter(edge -> Set.of("INCLUDES", "CONTAINS").contains(edge.relationType()))
        .filter(edge -> {
          KnowledgeGraphNode from = nodeByKey.get(edge.fromNodeKey());
          KnowledgeGraphNode to = nodeByKey.get(edge.toNodeKey());
          return from != null && to != null && from.level() == 1 && to.level() == 2;
        })
        .map(edge -> Map.<String, Object>of(
            "fromNodeKey", edge.fromNodeKey(),
            "toNodeKey", edge.toNodeKey(),
            "relationType", edge.relationType(),
            "visible", edge.visible()))
        .toList();
    return """
        请只为当前三级知识图谱节点选择最合理的二级栏目归属边。
        这是全图编排任务，一级个人节点与二级栏目之间的关系已经确定，不要输出 CORE、me、OWNS，也不要输出三级节点之间的证据边。
        输入输出契约：
        1. 当前节点必须是 level 2；只能从 sectionNodes 中选择 level 1 栏目作为 fromNodeKey。
        2. 输出方向必须是 二级栏目 -> 当前三级节点，即 fromNodeKey 为 sectionNodes 的 nodeKey，toNodeKey 为 currentNode.nodeKey。
        3. BLOG 归属技术博客时用 CONTAINS；SKILL/PROJECT/MODULE 归属履历、项目证据、面试助手等栏目时用 INCLUDES。
        4. 可以返回多个强相关二级栏目，但不要超过 sectionNodes 数量；如果 existingParentEdges 已经有某个 fromNodeKey，不要重复返回这条边，只补充缺失且合理的二级栏目。
        5. 如果当前节点已经有合理归属，也可以继续补充其他强相关栏目；如果没有明确新增归属，返回空数组，不要为了图谱好看造边。
        6. 禁止跨级连接，禁止输出 level2 -> level0、level0 -> level1、level2 -> level2。
        输出契约：返回严格 JSON 数组，不要 Markdown，不要解释文字，不要 <think>。
        JSON schema: [{"fromNodeKey":"section node key","toNodeKey":"current node key","relationType":"INCLUDES|CONTAINS","reason":"不超过60字的依据"}]。
        currentNode: %s
        sectionNodes: %s
        existingParentEdges: %s
        """.formatted(toJson(node), toJson(sectionNodes), toJson(existingParentEdges));
  }

  private String buildKnowledgeOrchestratePrompt(KnowledgeGraphNode node, KnowledgeGraphView graph) {
    Map<String, KnowledgeGraphNode> nodeByKey = new LinkedHashMap<>();
    graph.nodes().forEach(item -> nodeByKey.put(item.nodeKey(), item));
    List<Map<String, Object>> sectionNodes = graph.nodes().stream()
        .filter(item -> item.level() == 1)
        .map(item -> Map.<String, Object>of(
            "nodeKey", item.nodeKey(),
            "label", item.label(),
            "nodeType", item.nodeType(),
            "level", item.level(),
            "summary", item.summary(),
            "tags", item.tags()))
        .toList();
    List<Map<String, Object>> tertiaryNodes = graph.nodes().stream()
        .filter(item -> item.level() == 2)
        .filter(item -> !item.nodeKey().equals(node.nodeKey()))
        .map(item -> Map.<String, Object>of(
            "nodeKey", item.nodeKey(),
            "label", item.label(),
            "nodeType", item.nodeType(),
            "level", item.level(),
            "summary", item.summary(),
            "tags", item.tags()))
        .toList();
    List<Map<String, Object>> existingEdges = graph.edges().stream()
        .filter(edge -> edge.fromNodeKey().equals(node.nodeKey()) || edge.toNodeKey().equals(node.nodeKey()))
        .filter(edge -> {
          KnowledgeGraphNode from = nodeByKey.get(edge.fromNodeKey());
          KnowledgeGraphNode to = nodeByKey.get(edge.toNodeKey());
          return from != null && to != null;
        })
        .map(edge -> Map.<String, Object>of(
            "fromNodeKey", edge.fromNodeKey(),
            "toNodeKey", edge.toNodeKey(),
            "relationType", edge.relationType(),
            "visible", edge.visible()))
        .toList();
    return """
        请为当前三级知识图谱节点补齐“必要关系”，只能连接到 sectionNodes 或 tertiaryNodes 中存在的节点。
        这是全图编排任务，一级个人节点与二级栏目之间的关系已经确定，不要输出 CORE、me、OWNS。
        输入输出契约：
        1. 当前节点必须是 level 2；每条关系必须直接包含 currentNode，不要替其它两个节点新建关系。
        2. 归属边：只能是 二级栏目 -> 当前三级节点；fromNodeKey 来自 sectionNodes，toNodeKey 为 currentNode.nodeKey；BLOG 用 CONTAINS，其它三级内容用 INCLUDES。
        3. 证据边：只能是三级内容节点之间；PROJECT/MODULE -> SKILL 用 USES；BLOG -> SKILL/PROJECT/MODULE 用 EXPLAINS；只有标签、摘要或内容有明确重合时才用 RELATED。
        4. 可以同时返回归属边和证据边；不要重复 existingEdges 里已有的 fromNodeKey/toNodeKey/relationType。
        5. 没有明确新增关系时返回空数组，不要为了图谱好看造边。
        6. 禁止跨级连接，禁止输出 level2 -> level0、level0 -> level1、SECTION -> SECTION、level2 -> SECTION。
        输出契约：返回严格 JSON 数组，不要 Markdown，不要解释文字，不要 <think>。
        JSON schema: [{"fromNodeKey":"node key","toNodeKey":"node key","relationType":"INCLUDES|CONTAINS|USES|EXPLAINS|RELATED","reason":"不超过60字的依据"}]。
        currentNode: %s
        sectionNodes: %s
        tertiaryNodes: %s
        existingEdges: %s
        """.formatted(toJson(node), toJson(sectionNodes), toJson(tertiaryNodes), toJson(existingEdges));
  }

  private List<KnowledgeRelationSuggestion> parseRelationSuggestions(String content) {
    try {
      JsonNode json = extractJson(content);
      JsonNode array = json.isArray() ? json : json.path("relations");
      if (!array.isArray()) {
        throw ApiException.upstream("Minimax 图谱关系响应不是 JSON 数组。");
      }
      return objectMapper.convertValue(array, new TypeReference<List<KnowledgeRelationSuggestion>>() {});
    } catch (RuntimeException exception) {
      if (exception instanceof ApiException apiException) {
        throw apiException;
      }
      throw ApiException.upstream("Minimax 图谱关系响应解析失败：" + trimTo(exception.getMessage(), 180));
    }
  }

  private List<KnowledgeRelationSuggestion> sanitizeSuggestions(
      KnowledgeGraphNode node,
      KnowledgeGraphView graph,
      List<KnowledgeRelationSuggestion> suggestions) {
    Set<String> keys = new LinkedHashSet<>(graph.nodes().stream().map(KnowledgeGraphNode::nodeKey).toList());
    keys.add(node.nodeKey());
    return suggestions.stream()
        .filter(item -> keys.contains(item.fromNodeKey()) && keys.contains(item.toNodeKey()))
        .filter(item -> !item.fromNodeKey().equals(item.toNodeKey()))
        .map(item -> new KnowledgeRelationSuggestion(
            item.fromNodeKey(),
            item.toNodeKey(),
            allowedRelation(item.relationType()),
            nullToEmpty(item.reason())))
        .filter(item -> touchesNode(item, node.nodeKey()))
        .limit(8)
        .toList();
  }

  private JsonNode extractJson(String content) {
    try {
      return parseJsonContent(content);
    } catch (ApiException parseException) {
      String repaired = callMinimax("""
          下面是一段模型输出，目标是把它转换成严格 JSON。
          只输出 JSON，不要 Markdown，不要解释，不要 <think>。
          保留原有事实，不补造新信息；无法确定的字段用空字符串或空数组。
          原始输出：
          %s
          """.formatted(nullToEmpty(content)));
      try {
        return parseJsonContent(repaired);
      } catch (ApiException repairException) {
        throw ApiException.upstream("模型返回无法解析为 JSON，自动 JSON 修复也失败：" + trimTo(repairException.getMessage(), 160));
      }
    }
  }

  private JsonNode parseJsonContent(String content) {
    try {
      String normalized = stripReasoning(content == null ? "" : content.trim());
      normalized = normalized
          .replaceFirst("(?is)^```json\\s*", "")
          .replaceFirst("(?is)^```\\s*", "")
          .replaceFirst("(?is)```\\s*$", "")
          .trim();
      int startArray = normalized.indexOf('[');
      int startObject = normalized.indexOf('{');
      int start;
      if (startArray >= 0 && startObject >= 0) {
        start = Math.min(startArray, startObject);
      } else {
        start = Math.max(startArray, startObject);
      }
      int end = Math.max(normalized.lastIndexOf(']'), normalized.lastIndexOf('}'));
      if (start >= 0 && end > start) {
        normalized = normalized.substring(start, end + 1);
      }
      return objectMapper.readTree(normalized);
    } catch (Exception exception) {
      throw ApiException.upstream("模型返回无法解析为 JSON：" + trimTo(exception.getMessage(), 180));
    }
  }

  private List<String> strings(JsonNode json, String field) {
    JsonNode values = json.path(field);
    if (!values.isArray()) {
      return List.of();
    }
    List<String> result = new ArrayList<>();
    values.forEach(value -> {
      if (value.isTextual() && !value.asText().isBlank()) {
        result.add(value.asText());
      }
    });
    return result;
  }

  private String text(JsonNode json, String field, String defaultValue) {
    JsonNode value = json.path(field);
    return value.isTextual() && !value.asText().isBlank() ? value.asText() : defaultValue;
  }

  private String requiredText(JsonNode json, String field, String label) {
    JsonNode value = json.path(field);
    if (!value.isTextual() || value.asText().isBlank()) {
      throw ApiException.upstream("Minimax 响应缺少必需字段：" + label);
    }
    return value.asText().trim();
  }

  private List<String> requiredStrings(JsonNode json, String field, String label) {
    List<String> values = strings(json, field);
    if (values.isEmpty()) {
      throw ApiException.upstream("Minimax 响应缺少必需数组字段：" + label);
    }
    return values;
  }

  private int requiredInt(JsonNode json, String field, String label) {
    JsonNode value = json.path(field);
    if (!value.canConvertToInt()) {
      throw ApiException.upstream("Minimax 响应缺少必需数字字段：" + label);
    }
    return value.asInt();
  }

  private ResumeBasicInfoRequest basicInfoFromJson(JsonNode json, String rawText) {
    String name = text(json, "name", "");
    String title = text(json, "title", "");
    String summary = text(json, "summary", "");
    String education = text(json, "education", "");
    if (name.isBlank() || title.isBlank() || summary.isBlank() || education.isBlank()) {
      throw ApiException.upstream("Minimax 简历扫描缺少 name/title/summary/education，已拒绝生成占位信息。");
    }
    return new ResumeBasicInfoRequest(
        name,
        title,
        summary,
        text(json, "email", ""),
        text(json, "phone", ""),
        text(json, "location", ""),
        education,
        text(json, "githubUrl", ""),
        text(json, "websiteUrl", ""));
  }

  private List<ResumeSectionItemRequest> sectionItemsFromJson(ResumeSectionType type, JsonNode array) {
    if (!array.isArray()) {
      return List.of();
    }
    List<ResumeSectionItemRequest> items = new ArrayList<>();
    int nextOrder = 1;
    for (JsonNode item : array) {
      String title = text(item, "title", "");
      if (title.isBlank()) {
        continue;
      }
      items.add(new ResumeSectionItemRequest(
          trimTo(title, 160),
          trimTo(text(item, "subtitle", ""), 160),
          trimTo(text(item, "period", ""), 64),
          text(item, "summary", ""),
          text(item, "detail", text(item, "summary", "")),
          strings(item, "tags"),
          item.path("visible").isMissingNode() || item.path("visible").asBoolean(true),
          item.path("sortOrder").asInt(nextOrder)));
      nextOrder++;
    }
    return items;
  }

  private List<ProjectRecommendation> projectRecommendations(JsonNode array, List<Project> projects, Set<String> allowedSlugs) {
    if (!array.isArray()) return List.of();
    Map<String, Project> bySlug = projects.stream().collect(java.util.stream.Collectors.toMap(Project::slug, item -> item, (a, b) -> a));
    List<ProjectRecommendation> result = new ArrayList<>();
    for (JsonNode item : array) {
      String slug = text(item, "slug", "");
      if (!allowedSlugs.contains(slug)) continue;
      Project project = bySlug.get(slug);
      result.add(new ProjectRecommendation(
          slug,
          project == null ? slug : project.name(),
          requiredText(item, "emphasis", "projectRecommendations.emphasis"),
          strings(item, "supportedBy")));
    }
    return result;
  }

  private List<ModuleRecommendation> moduleRecommendations(JsonNode array, List<ModuleDemo> modules, Set<String> allowedSlugs) {
    if (!array.isArray()) return List.of();
    Map<String, ModuleDemo> bySlug = modules.stream().collect(java.util.stream.Collectors.toMap(ModuleDemo::slug, item -> item, (a, b) -> a));
    List<ModuleRecommendation> result = new ArrayList<>();
    for (JsonNode item : array) {
      String slug = text(item, "slug", "");
      if (!allowedSlugs.contains(slug)) continue;
      ModuleDemo module = bySlug.get(slug);
      result.add(new ModuleRecommendation(
          slug,
          module == null ? slug : module.title(),
          requiredText(item, "reason", "moduleRecommendations.reason")));
    }
    return result;
  }

  private int clampInt(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private String fallbackSummary(String resumeText, String role) {
    String prefix = "面向" + role + "，建议突出真实工程证据、技术栈匹配和可复盘结果。";
    if (resumeText != null && resumeText.contains("Redis")) {
      return prefix + " 可优先强调 Redis、MySQL、Spring Boot 等后端能力。";
    }
    return prefix;
  }

  private String fallbackGeneratedResume(String resumeText, String jdText, String role, List<String> hints) {
    List<String> keywords = fallbackKeywords(jdText, hints);
    String evidence = trimTo(nullToEmpty(resumeText), 3600);
    return """
        # JD 适配简历草案

        ## 求职方向
        %s

        ## 简历摘要
        %s

        ## 关键词对齐
        %s

        ## 项目与经历证据
        %s

        ## 投递前人工确认
        - 本草案来自本地辅助模板，请人工确认后使用。
        - 已保留原始简历证据，请人工补充真实量化指标、项目职责边界和可验证链接。
        - JD 中没有简历证据支撑的要求，不应写入正式简历正文。
        """.formatted(
            role,
            fallbackSummary(resumeText, role),
            keywords.isEmpty()
                ? "- 暂未从 JD 或资产提示中提取到稳定关键词。"
                : keywords.stream().map(item -> "- " + item).collect(java.util.stream.Collectors.joining("\n")),
            evidence.isBlank() ? "原始简历内容为空。" : evidence);
  }

  private List<String> fallbackKeywords(String jdText, List<String> hints) {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    String source = (nullToEmpty(jdText) + " " + String.join(" ", hints)).toLowerCase(Locale.ROOT);
    Map<String, String> candidates = Map.ofEntries(
        Map.entry("spring boot", "Spring Boot"),
        Map.entry("springcloud", "Spring Cloud"),
        Map.entry("spring cloud", "Spring Cloud"),
        Map.entry("redis", "Redis"),
        Map.entry("mysql", "MySQL"),
        Map.entry("java", "Java"),
        Map.entry("微服务", "微服务"),
        Map.entry("rag", "RAG"),
        Map.entry("ai", "AI 应用"),
        Map.entry("docker", "Docker"),
        Map.entry("kubernetes", "Kubernetes"),
        Map.entry("kafka", "Kafka"),
        Map.entry("缓存", "缓存优化"),
        Map.entry("并发", "并发控制"),
        Map.entry("接口", "接口设计"));
    candidates.forEach((needle, label) -> {
      if (source.contains(needle)) {
        result.add(label);
      }
    });
    hints.stream()
        .filter(item -> item != null && !item.isBlank())
        .limit(4)
        .forEach(item -> result.add(trimTo(item, 60)));
    return result.stream().limit(10).toList();
  }

  private List<MockInterviewQuestion> fallbackInterviewQuestions(String role, List<String> hints) {
    String asset = hints.isEmpty() ? "你最有把握的项目" : hints.get(0);
    List<MockInterviewQuestion> questions = new ArrayList<>();
    questions.add(new MockInterviewQuestion(
        "请用 1 分钟介绍自己，并说明为什么匹配" + role + "。",
        "验证候选人能否把教育背景、技术栈和岗位要求收束成清晰主线。",
        "我会先用一句话定位自己：我主要面向 Java 后端与工程化场景，项目里重点使用 Spring Boot、Redis、MySQL 等技术解决真实业务问题。和这个岗位匹配的地方在于，我不仅能完成接口开发，也会关注缓存一致性、数据落库、异常处理和可复盘的项目证据。接下来我会优先讲最能支撑 JD 要求的项目，再补充相关技术细节。",
        List.of("你认为自己和其他后端候选人的差异是什么？", "如果只保留一个项目讲给面试官，你会选哪个？"),
        List.of("岗位关键词", "真实项目证据", "表达结构", "不夸大经历")));
    questions.add(new MockInterviewQuestion(
        "请展开讲一下 " + asset + "，你负责了什么，难点在哪里？",
        "验证项目职责边界、技术选型和问题拆解能力。",
        "我会按背景、职责、方案、结果来讲。背景是项目中存在一个明确业务问题；我负责其中的后端模块实现和联调。难点不只是写接口，而是要考虑数据状态、缓存与数据库之间的一致性，以及异常情况下的处理方案。方案上我会先保证核心链路可观测，再把高频读写放到合适的位置优化。结果部分我会只说简历中真实可验证的指标或现象，没有数据的地方会说明是体验或稳定性改善。",
        List.of("这个方案有没有替代选型？", "你负责的代码边界具体到哪些接口或表？"),
        List.of("职责边界", "技术取舍", "业务结果", "可验证细节")));
    questions.add(new MockInterviewQuestion(
        "如果 Redis 缓存和 MySQL 数据出现不一致，你会如何排查和修复？",
        "考察后端排障、缓存策略和一致性理解。",
        "我会先确认不一致的范围和时间窗口，再看写入链路是否存在异常重试、事务提交后缓存更新失败、并发覆盖或过期策略问题。短期修复上，可以通过日志、数据对账和补偿任务恢复数据；长期方案会根据业务选择先写库再删缓存、延迟双删、消息补偿或幂等更新。回答时我会结合项目中真实用过的 Redis 场景说明，而不是泛泛背方案。",
        List.of("为什么不是先删缓存再写库？", "如何保证补偿任务不会重复写坏数据？"),
        List.of("排查顺序", "一致性模型", "幂等", "项目关联")));
    questions.add(new MockInterviewQuestion(
        "JD 提到 AI/RAG 加分项时，你会怎样把自己的项目讲得可信？",
        "验证候选人能否处理加分项和证据不足的边界。",
        "我会明确区分已经落地的工程能力和正在学习验证的 AI 能力。如果项目里只是做了 Agent Workflow 或 RAG Demo，我会说清楚输入输出契约、工具边界、失败处理和日志观测，而不会声称做过真实线上大规模 RAG。这样既能展示我对 AI 工程化的理解，也不会突破简历真实性边界。",
        List.of("RAG 和普通关键词检索有什么区别？", "LLM 接口不可用时产品应该如何处理？"),
        List.of("边界意识", "工程化理解", "失败处理", "真实性")));
    questions.add(new MockInterviewQuestion(
        "请说一个你项目中做过的优化点，为什么它是必要的？",
        "考察问题意识、优化前后对比和复盘能力。",
        "我会先说优化前的痛点，例如响应慢、重复查询、状态不同步或用户体验不稳定。然后解释我如何定位原因，比如通过日志、接口响应、SQL 或缓存命中情况确认瓶颈。方案会控制在我真实做过的范围内，例如缓存进度、减少重复落库、优化数据结构或补充异常处理。最后补充结果和反思：哪些指标改善了，哪些地方还需要更严谨的数据支撑。",
        List.of("你如何证明这个优化有效？", "如果数据量放大 10 倍，方案会有什么问题？"),
        List.of("问题定义", "定位证据", "方案约束", "复盘意识")));
    return questions;
  }

  private List<MockInterviewQuestion> interviewQuestions(JsonNode array) {
    if (!array.isArray()) {
      return List.of();
    }
    List<MockInterviewQuestion> result = new ArrayList<>();
    for (JsonNode item : array) {
      String question = text(item, "question", "");
      String strongAnswer = text(item, "strongAnswer", "");
      if (question.isBlank() || strongAnswer.isBlank()) {
        continue;
      }
      result.add(new MockInterviewQuestion(
          trimTo(question, 600),
          trimTo(text(item, "intent", ""), 600),
          strongAnswer.trim(),
          strings(item, "followUps"),
          strings(item, "scoreFocus")));
    }
    return result;
  }

  private void assertInterviewRespectsSources(List<MockInterviewQuestion> questions, String sourceText) {
    for (MockInterviewQuestion question : questions) {
      assertNoUnsafeGeneratedText("模拟面试 strongAnswer", question.strongAnswer(), sourceText);
    }
  }

  private void assertNoUnsafeGeneratedText(String label, String value, String sourceText) {
    String normalized = nullToEmpty(value);
    if (normalized.matches("(?s).*\\[[^\\]]{1,40}\\].*")) {
      throw ApiException.upstream("Minimax 已调用，但" + label + "包含占位符，已拒绝返回可能误导用户的结果。");
    }
    String lowered = normalized.toLowerCase(Locale.ROOT);
    List<String> unsafeTokens = List.of(
        "xxx",
        "学校名称",
        "专业名称",
        "公司名称",
        "某公司",
        "某项目",
        "示例项目",
        "通用回答",
        "未提供具体",
        "假设你",
        "学生信息管理系统");
    String source = nullToEmpty(sourceText).toLowerCase(Locale.ROOT);
    for (String token : unsafeTokens) {
      String loweredToken = token.toLowerCase(Locale.ROOT);
      if (lowered.contains(loweredToken) && !source.contains(loweredToken)) {
        throw ApiException.upstream("Minimax 已调用，但" + label + "包含疑似占位或编造内容：" + token + "；已拒绝返回替代结果。");
      }
    }
  }

  private String trimTo(String value, int maxLength) {
    String normalized = value == null ? "" : value.trim();
    return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
  }

  private String normalizeCompare(String value) {
    return nullToEmpty(value).replaceAll("\\s+", "").trim();
  }

  private void ensureConfigured() {
    if (!configured()) {
      throw ApiException.serviceUnavailable("未配置 MINIMAX_API_KEY，AI 接口不会生成替代结果或返回假数据。");
    }
    if (!authFailureMessage.isBlank()) {
      throw ApiException.serviceUnavailable("Minimax API Key 当前不可用，AI 接口不会生成替代结果或返回假数据：" + authFailureMessage);
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception exception) {
      return String.valueOf(value);
    }
  }

  private String allowedRelation(String relationType) {
    String normalized = relationType == null ? "RELATED" : relationType.trim().toUpperCase(Locale.ROOT);
    return Set.of("OWNS", "INCLUDES", "CONTAINS", "USES", "EXPLAINS", "RELATED").contains(normalized)
        ? normalized
        : "RELATED";
  }

  private boolean touchesNode(KnowledgeRelationSuggestion suggestion, String nodeKey) {
    return nodeKey.equals(suggestion.fromNodeKey()) || nodeKey.equals(suggestion.toNodeKey());
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String plainAgentAnswer(String content) {
    String normalized = content == null ? "" : content.trim();
    try {
      JsonNode json = parseJsonContent(normalized);
      String response = text(json, "response", "");
      if (!response.isBlank()) {
        return response;
      }
      String answer = text(json, "answer", "");
      if (!answer.isBlank()) {
        return answer;
      }
    } catch (ApiException ignored) {
      // Natural-language agent answers are allowed; JSON parsing is only a cleanup path.
    }
    return normalized;
  }

  private TokenUsage tokenUsage(JsonNode root, int fallbackInputTokens, int fallbackOutputTokens) {
    JsonNode usage = root.path("usage");
    int inputTokens = firstPositiveInt(
        usage.path("prompt_tokens").asInt(0),
        usage.path("input_tokens").asInt(0),
        usage.path("total_input_tokens").asInt(0),
        fallbackInputTokens);
    int outputTokens = firstPositiveInt(
        usage.path("completion_tokens").asInt(0),
        usage.path("output_tokens").asInt(0),
        usage.path("total_output_tokens").asInt(0),
        fallbackOutputTokens);
    int totalTokens = usage.path("total_tokens").asInt(0);
    if (totalTokens > 0 && outputTokens <= 0 && inputTokens > 0 && totalTokens > inputTokens) {
      outputTokens = totalTokens - inputTokens;
    }
    return new TokenUsage(inputTokens, outputTokens);
  }

  private int estimateTokens(String value) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isBlank()) {
      return 0;
    }
    int cjk = 0;
    int ascii = 0;
    for (int i = 0; i < normalized.length(); i++) {
      char ch = normalized.charAt(i);
      if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
        cjk++;
      } else if (!Character.isWhitespace(ch)) {
        ascii++;
      }
    }
    return Math.max(1, cjk + (int) Math.ceil(ascii / 4.0));
  }

  private int firstPositiveInt(int... values) {
    for (int value : values) {
      if (value > 0) {
        return value;
      }
    }
    return 0;
  }

  private String stripReasoning(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    String stripped = value.replaceAll("(?is)<think>.*?</think>", "").trim();
    return stripped.isBlank() ? value.trim() : stripped;
  }

  private String trimTrailingSlash(String value) {
    while (value.endsWith("/")) {
      value = value.substring(0, value.length() - 1);
    }
    return value;
  }

  private enum RelationTask {
    FULL,
    TERTIARY_PARENT_ONLY,
    TERTIARY_ORCHESTRATE
  }

  private record TokenUsage(int inputTokens, int outputTokens) {
  }
}
