package com.mystyle.portfolio.moduleDemo;

import com.mystyle.portfolio.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lab/agent-workflow")
public class AgentWorkflowController {
  private final AgentWorkflowService agentWorkflowService;

  public AgentWorkflowController(AgentWorkflowService agentWorkflowService) {
    this.agentWorkflowService = agentWorkflowService;
  }

  @GetMapping("/templates")
  public ApiResponse<List<Map<String, Object>>> templates() {
    return ApiResponse.success(agentWorkflowService.templates());
  }

  @PostMapping("/run")
  public ApiResponse<AgentWorkflowRun> run(@Valid @RequestBody AgentWorkflowRequest request) {
    return ApiResponse.success(agentWorkflowService.run(request));
  }

  @GetMapping("/runs/{runId}")
  public ApiResponse<AgentWorkflowRun> getRun(@PathVariable("runId") String runId) {
    return ApiResponse.success(agentWorkflowService.getRun(runId));
  }
}
