package com.mystyle.portfolio.moduleDemo;

import com.mystyle.portfolio.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/lab/agent-workflow")
public class AgentWorkflowController {
  private final AgentWorkflowService agentWorkflowService;
  private final ObjectMapper objectMapper;

  public AgentWorkflowController(AgentWorkflowService agentWorkflowService, ObjectMapper objectMapper) {
    this.agentWorkflowService = agentWorkflowService;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/templates")
  public ApiResponse<List<Map<String, Object>>> templates() {
    return ApiResponse.success(agentWorkflowService.templates());
  }

  @PostMapping("/run")
  public ApiResponse<AgentWorkflowRun> run(@Valid @RequestBody AgentWorkflowRequest request) {
    return ApiResponse.success(agentWorkflowService.run(request));
  }

  @PostMapping(
      value = "/run/stream",
      produces = MediaType.APPLICATION_NDJSON_VALUE)
  public ResponseEntity<StreamingResponseBody> runStream(@Valid @RequestBody AgentWorkflowRequest request) {
    StreamingResponseBody body = outputStream -> {
      AgentWorkflowRun run = agentWorkflowService.stream(request, delta -> {
        try {
          writeEvent(outputStream, "delta", "模型输出", delta, null);
        } catch (IOException exception) {
          throw new java.io.UncheckedIOException(exception);
        }
      });
      writeEvent(outputStream, "final", "完成", "语言助手已完成回答。", run);
    };
    return streamResponse(body);
  }

  private ResponseEntity<StreamingResponseBody> streamResponse(StreamingResponseBody body) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_NDJSON)
        .cacheControl(CacheControl.noCache())
        .header(HttpHeaders.CONNECTION, "keep-alive")
        .header("X-Accel-Buffering", "no")
        .header("Cache-Control", "no-cache, no-transform")
        .body(body);
  }

  @GetMapping("/runs/{runId}")
  public ApiResponse<AgentWorkflowRun> getRun(@PathVariable("runId") String runId) {
    return ApiResponse.success(agentWorkflowService.getRun(runId));
  }

  private void writeEvent(
      java.io.OutputStream outputStream,
      String type,
      String step,
      String message,
      Object data) throws IOException {
    String line = objectMapper.writeValueAsString(Map.of(
        "type", type,
        "step", step,
        "message", message,
        "data", data == null ? Map.of() : data)) + "\n";
    outputStream.write(line.getBytes(StandardCharsets.UTF_8));
    outputStream.flush();
  }
}
