DROP TABLE IF EXISTS ai_chat_history CASCADE;
DROP TABLE IF EXISTS education CASCADE;
DROP TABLE IF EXISTS experience CASCADE;
DROP TABLE IF EXISTS honor CASCADE;
DROP TABLE IF EXISTS portfolio CASCADE;
DROP TABLE IF EXISTS profile CASCADE;
DROP TABLE IF EXISTS resume_version CASCADE;
DROP TABLE IF EXISTS share_link CASCADE;
DROP TABLE IF EXISTS site_config CASCADE;
DROP TABLE IF EXISTS skill CASCADE;
DROP TABLE IF EXISTS user CASCADE;
DROP TABLE IF EXISTS visit_log CASCADE;

CREATE TABLE ai_chat_history (
  id bigint NOT NULL AUTO_INCREMENT,
  session_id varchar(64) DEFAULT NULL,
  question text,
  answer text,
  source varchar(20) DEFAULT 'local',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE education (
  id bigint NOT NULL AUTO_INCREMENT,
  version_id bigint NOT NULL DEFAULT 1,
  school varchar(100) DEFAULT NULL,
  major varchar(100) DEFAULT NULL,
  degree varchar(50) DEFAULT NULL,
  start_date date DEFAULT NULL,
  end_date date DEFAULT NULL,
  description text,
  sort int NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE experience (
  id bigint NOT NULL AUTO_INCREMENT,
  version_id bigint NOT NULL DEFAULT 1,
  type tinyint DEFAULT 1,
  company varchar(100) DEFAULT NULL,
  position varchar(100) DEFAULT NULL,
  start_date date DEFAULT NULL,
  end_date date DEFAULT NULL,
  description text,
  tech_stack varchar(500) DEFAULT NULL,
  sort int NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE honor (
  id bigint NOT NULL AUTO_INCREMENT,
  owner_name varchar(50) NOT NULL,
  title varchar(200) NOT NULL,
  image varchar(255) DEFAULT NULL,
  issuer varchar(100) DEFAULT NULL,
  level varchar(50) DEFAULT NULL,
  honor_date date DEFAULT NULL,
  description varchar(500) DEFAULT NULL,
  sort int NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE portfolio (
  id bigint NOT NULL AUTO_INCREMENT,
  owner_name varchar(50) NOT NULL,
  title varchar(100) DEFAULT NULL,
  cover varchar(255) DEFAULT NULL,
  url varchar(255) DEFAULT NULL,
  description text,
  sort int NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE profile (
  id bigint NOT NULL AUTO_INCREMENT,
  version_id bigint NOT NULL DEFAULT 1,
  name varchar(50) DEFAULT NULL,
  job_title varchar(100) DEFAULT NULL,
  slogan varchar(255) DEFAULT NULL,
  avatar varchar(255) DEFAULT NULL,
  email varchar(100) DEFAULT NULL,
  phone varchar(20) DEFAULT NULL,
  wechat varchar(50) DEFAULT NULL,
  github varchar(255) DEFAULT NULL,
  gitee varchar(255) DEFAULT NULL,
  csdn varchar(255) DEFAULT NULL,
  address varchar(100) DEFAULT NULL,
  about text,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE resume_version (
  id bigint NOT NULL AUTO_INCREMENT,
  version_name varchar(100) NOT NULL,
  description varchar(500) DEFAULT NULL,
  is_default tinyint DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (is_default)
);

CREATE TABLE share_link (
  id bigint NOT NULL AUTO_INCREMENT,
  token varchar(64) NOT NULL,
  remark varchar(100) DEFAULT NULL,
  version_id bigint DEFAULT NULL,
  expire_time datetime DEFAULT NULL,
  max_views int DEFAULT NULL,
  view_count int NOT NULL DEFAULT 0,
  enabled tinyint NOT NULL DEFAULT 1,
  last_view_time datetime DEFAULT NULL,
  last_view_ip varchar(50) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (token)
);

CREATE TABLE site_config (
  id bigint NOT NULL,
  site_title varchar(100) DEFAULT '个人简历',
  default_theme varchar(50) DEFAULT 'default',
  ai_enabled tinyint NOT NULL DEFAULT 1,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE skill (
  id bigint NOT NULL AUTO_INCREMENT,
  version_id bigint NOT NULL DEFAULT 1,
  category varchar(50) DEFAULT NULL,
  name varchar(50) DEFAULT NULL,
  level int DEFAULT 80,
  sort int NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE user (
  id bigint NOT NULL AUTO_INCREMENT,
  username varchar(50) NOT NULL,
  password varchar(100) NOT NULL,
  nickname varchar(50) DEFAULT NULL,
  avatar varchar(255) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (username)
);

CREATE TABLE visit_log (
  id bigint NOT NULL AUTO_INCREMENT,
  ip varchar(64) DEFAULT NULL,
  user_agent varchar(500) DEFAULT NULL,
  referer varchar(500) DEFAULT NULL,
  path varchar(255) DEFAULT NULL,
  session_id varchar(64) DEFAULT NULL,
  visit_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE INDEX idx_ai_session ON ai_chat_history(session_id);
CREATE INDEX idx_ai_create_time ON ai_chat_history(create_time);
CREATE INDEX idx_education_version ON education(version_id);
CREATE INDEX idx_experience_version_type ON experience(version_id, type);
CREATE INDEX idx_honor_owner ON honor(owner_name);
CREATE INDEX idx_portfolio_owner ON portfolio(owner_name);
CREATE INDEX idx_profile_version ON profile(version_id);
CREATE INDEX idx_share_enabled ON share_link(enabled);
CREATE INDEX idx_skill_version ON skill(version_id);
CREATE INDEX idx_visit_time ON visit_log(visit_time);
CREATE INDEX idx_visit_ip ON visit_log(ip);