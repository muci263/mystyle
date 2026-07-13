package com.mystyle.portfolio.moduleDemo;

import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.llm.LlmService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AgentWorkflowService {
  private final Map<String, AgentWorkflowRun> runs = new ConcurrentHashMap<>();
  private final LlmService llmService;

  public AgentWorkflowService(LlmService llmService) {
    this.llmService = llmService;
  }

  public List<Map<String, Object>> templates() {
    return List.of(
        Map.of(
            "templateId", "rag-question",
            "name", "RAG 知识库问答",
            "nodes", List.of("intent", "retrieval", "llm", "verify")),
        Map.of(
            "templateId", "sql-bot",
            "name", "SQL 智能问答",
            "nodes", List.of("intent", "metadata", "sql_safety", "execute", "explain")));
  }

  public AgentWorkflowRun run(AgentWorkflowRequest request) {
    String templateId = request.templateId() == null || request.templateId().isBlank()
        ? "rag-question"
        : request.templateId();
    List<AgentWorkflowStep> steps = buildSteps(templateId);
    String answer = llmService.runAgentWorkflow(request.question(), templateId, request.memory());
    AgentWorkflowRun run = new AgentWorkflowRun(UUID.randomUUID().toString(), "COMPLETED", request.question(), answer, steps);
    runs.put(run.runId(), run);
    return run;
  }

  public AgentWorkflowRun stream(AgentWorkflowRequest request, java.util.function.Consumer<String> onDelta) {
    String templateId = request.templateId() == null || request.templateId().isBlank()
        ? "rag-question"
        : request.templateId();
    List<AgentWorkflowStep> steps = buildSteps(templateId);
    String answer = llmService.streamAgentWorkflow(request.question(), templateId, request.memory(), onDelta);
    AgentWorkflowRun run = new AgentWorkflowRun(UUID.randomUUID().toString(), "COMPLETED", request.question(), answer, steps);
    runs.put(run.runId(), run);
    return run;
  }

  public AgentWorkflowRun getRun(String runId) {
    AgentWorkflowRun run = runs.get(runId);
    if (run == null) {
      throw ApiException.notFound("Agent 运行记录不存在");
    }
    return run;
  }

  private List<AgentWorkflowStep> buildSteps(String templateId) {
    if ("sql-bot".equals(templateId)) {
      return List.of(
          new AgentWorkflowStep("intent", "DONE", "识别为数据库问答任务"),
          new AgentWorkflowStep("metadata", "DONE", "读取表结构与字段说明"),
          new AgentWorkflowStep("sql_safety", "DONE", "仅允许 SELECT 演示查询"),
          new AgentWorkflowStep("execute", "SKIPPED", "未接入真实数据库执行器，不生成伪造查询结果"),
          new AgentWorkflowStep("explain", "DONE", "Minimax 基于问题和工具边界生成解释"));
    }
    return List.of(
        new AgentWorkflowStep("intent", "DONE", "识别用户问题意图"),
        new AgentWorkflowStep("retrieval", "SKIPPED", "未接入真实 RAG 检索器，不生成伪造召回片段"),
        new AgentWorkflowStep("llm", "DONE", "调用 Minimax 生成回答"),
        new AgentWorkflowStep("verify", "DONE", "检查回答是否有来源支撑"));
  }
}
