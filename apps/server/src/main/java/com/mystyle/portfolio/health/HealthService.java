package com.mystyle.portfolio.health;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {
  private final JdbcTemplate jdbcTemplate;

  public HealthService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public HealthStatus health() {
    Map<String, String> components = new LinkedHashMap<>();
    components.put("contentRepository", "MYSQL_JDBC");
    components.put("mysql", mysqlStatus());
    components.put("redis", "SIMULATED_BY_MEMORY");
    components.put("llmProvider", "MOCK_RULE_PROVIDER");
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
