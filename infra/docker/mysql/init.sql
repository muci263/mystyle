CREATE TABLE IF NOT EXISTS schema_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version VARCHAR(64) NOT NULL,
  description VARCHAR(255) NOT NULL,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_version (version, description)
VALUES ('0.2.0', 'mysql content repository')
ON DUPLICATE KEY UPDATE description = description;
