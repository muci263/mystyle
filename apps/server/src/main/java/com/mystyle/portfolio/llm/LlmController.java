package com.mystyle.portfolio.llm;

import com.mystyle.portfolio.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/llm")
public class LlmController {
  private final LlmService llmService;
  private final ObjectMapper objectMapper;

  public LlmController(LlmService llmService, ObjectMapper objectMapper) {
    this.llmService = llmService;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/status")
  public ApiResponse<LlmProviderStatus> status() {
    return ApiResponse.success(llmService.status());
  }

  @PostMapping("/resume/optimize")
  public ApiResponse<ResumeOptimizeResponse> optimizeResume(@Valid @RequestBody ResumeOptimizeRequest request) {
    return ApiResponse.success(llmService.optimizeResume(request));
  }

  @PostMapping("/interview/mock")
  public ApiResponse<MockInterviewResponse> mockInterview(@Valid @RequestBody MockInterviewRequest request) {
    return ApiResponse.success(llmService.mockInterview(request));
  }

  @PostMapping("/interview/session/question")
  public ApiResponse<InterviewTurnResponse> nextInterviewQuestion(@Valid @RequestBody InterviewTurnRequest request) {
    return ApiResponse.success(llmService.nextInterviewQuestion(request));
  }

  @PostMapping(
      value = "/interview/session/finalize/stream",
      produces = MediaType.APPLICATION_NDJSON_VALUE)
  public ResponseEntity<StreamingResponseBody> finalizeInterviewStream(@Valid @RequestBody InterviewFinalizeRequest request) {
    StreamingResponseBody body = outputStream -> {
      writeEvent(outputStream, "progress", "输入输出契约", "已锁定 JD、简历、问答记录和 Markdown 简历输出。", null);
      writeEvent(outputStream, "progress", "面试复盘", "正在整理一问一答记录，识别表达亮点和风险缺口。", null);
      writeEvent(outputStream, "progress", "适配简历", "正在调用 Minimax 生成 JD 适配版 Markdown 简历。", null);
      InterviewFinalizeResponse response = llmService.finalizeInterview(request);
      writeEvent(outputStream, "progress", "结果封装", "已生成摘要、适配简历、反馈和下一步建议。", null);
      writeEvent(outputStream, "final", "完成", "面试结果包已生成。", response);
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
