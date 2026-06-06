CREATE TABLE IF NOT EXISTS schema_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version VARCHAR(64) NOT NULL UNIQUE,
  description VARCHAR(255) NOT NULL,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  summary TEXT NOT NULL,
  email VARCHAR(128) NOT NULL,
  education VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS profile_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  profile_id BIGINT NOT NULL,
  tag VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_profile_tag_profile FOREIGN KEY (profile_id) REFERENCES profile(id)
);

CREATE TABLE IF NOT EXISTS skill_group (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS skill_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  group_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_skill_item_group FOREIGN KEY (group_id) REFERENCES skill_group(id)
);

CREATE TABLE IF NOT EXISTS experience (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company VARCHAR(128) NOT NULL,
  position VARCHAR(128) NOT NULL,
  period VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS experience_highlight (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  experience_id BIGINT NOT NULL,
  highlight TEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_experience_highlight_experience FOREIGN KEY (experience_id) REFERENCES experience(id)
);

CREATE TABLE IF NOT EXISTS project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_index VARCHAR(16) NOT NULL,
  slug VARCHAR(128) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  summary TEXT NOT NULL,
  role VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_tech (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  tech VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_project_tech_project FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE IF NOT EXISTS project_metric (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  metric VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_project_metric_project FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE IF NOT EXISTS project_responsibility (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  responsibility TEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_project_responsibility_project FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE IF NOT EXISTS project_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  problem TEXT NOT NULL,
  solution TEXT NOT NULL,
  result TEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_project_evidence_project FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE IF NOT EXISTS module_demo (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  slug VARCHAR(128) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  title VARCHAR(128) NOT NULL,
  demo_type VARCHAR(64) NOT NULL,
  project VARCHAR(128) NOT NULL,
  summary TEXT NOT NULL,
  api_base VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS module_demo_tech (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  module_id BIGINT NOT NULL,
  tech VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_module_demo_tech_module FOREIGN KEY (module_id) REFERENCES module_demo(id)
);

CREATE TABLE IF NOT EXISTS module_demo_talking_point (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  module_id BIGINT NOT NULL,
  talking_point TEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_module_demo_talking_point_module FOREIGN KEY (module_id) REFERENCES module_demo(id)
);

CREATE TABLE IF NOT EXISTS interview_guide (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  short_intro TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS interview_project_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guide_id BIGINT NOT NULL,
  item_value VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_interview_project_order_guide FOREIGN KEY (guide_id) REFERENCES interview_guide(id)
);

CREATE TABLE IF NOT EXISTS interview_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guide_id BIGINT NOT NULL,
  item_value TEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_interview_question_guide FOREIGN KEY (guide_id) REFERENCES interview_guide(id)
);

CREATE TABLE IF NOT EXISTS interview_open_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guide_id BIGINT NOT NULL,
  item_value VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_interview_open_link_guide FOREIGN KEY (guide_id) REFERENCES interview_guide(id)
);

CREATE TABLE IF NOT EXISTS blog_post (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  slug VARCHAR(128) NOT NULL UNIQUE,
  title VARCHAR(160) NOT NULL,
  excerpt TEXT NOT NULL,
  content TEXT NOT NULL,
  category VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'published',
  published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_minutes INT NOT NULL DEFAULT 4,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS blog_post_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  tag VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_blog_post_tag_post FOREIGN KEY (post_id) REFERENCES blog_post(id)
);

CREATE TABLE IF NOT EXISTS blog_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  author VARCHAR(64) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'visible',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_blog_comment_post FOREIGN KEY (post_id) REFERENCES blog_post(id)
);

CREATE TABLE IF NOT EXISTS blog_annotation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  anchor_text VARCHAR(160) NOT NULL,
  note TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_blog_annotation_post FOREIGN KEY (post_id) REFERENCES blog_post(id)
);

CREATE TABLE IF NOT EXISTS blog_like (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  client_key VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_blog_like_post FOREIGN KEY (post_id) REFERENCES blog_post(id)
);

CREATE TABLE IF NOT EXISTS resume_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  source_task_id BIGINT NULL,
  published_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resume_basic_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  summary TEXT NOT NULL,
  email VARCHAR(128) NOT NULL,
  phone VARCHAR(64) NULL,
  location VARCHAR(128) NULL,
  education VARCHAR(128) NOT NULL,
  github_url VARCHAR(255) NULL,
  website_url VARCHAR(255) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_resume_basic_info_version FOREIGN KEY (version_id) REFERENCES resume_version(id)
);

CREATE TABLE IF NOT EXISTS resume_section_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version_id BIGINT NOT NULL,
  section_type VARCHAR(32) NOT NULL,
  title VARCHAR(160) NOT NULL,
  subtitle VARCHAR(160) NULL,
  period VARCHAR(64) NULL,
  summary TEXT NULL,
  detail TEXT NULL,
  tags TEXT NULL,
  visible TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_resume_section_item_version FOREIGN KEY (version_id) REFERENCES resume_version(id)
);

CREATE TABLE IF NOT EXISTS resume_upload_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  raw_text LONGTEXT NOT NULL,
  parsed_json LONGTEXT NULL,
  error_message TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
