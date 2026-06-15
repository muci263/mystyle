package com.mystyle.portfolio.health;

import com.mystyle.portfolio.llm.LlmService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {
  private final JdbcTemplate jdbcTemplate;
  private final LlmService llmService;

  public HealthService(JdbcTemplate jdbcTemplate, LlmService llmService) {
    this.jdbcTemplate = jdbcTemplate;
    this.llmService = llmService;
  }

  public HealthStatus health() {
    Map<String, String> components = new LinkedHashMap<>();
    components.put("contentRepository", "MYSQL_JDBC");
    components.put("mysql", mysqlStatus());
    components.put("redis", "SIMULATED_BY_MEMORY");
    components.put("llmProvider", llmService.providerName());
    components.put("llmConfigured", String.valueOf(llmService.configured()));
    components.put("swagger", "READY");
    return new HealthStatus("UP", "portfolio-server", components);
  }

  private String mysqlStatus() {
    try {
      Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      return result != null && result == 1 ? "CONNECTED" : "UNKNOWN";
    } catch (Exception exception) {
      return "ERROR";
    }
  }
}
