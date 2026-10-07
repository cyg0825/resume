-- 建表脚本：通过 JDBC URL 的 INIT=RUNSCRIPT 在每次连接时执行，因此全部语句必须幂等。
-- 不写 DROP TABLE——一旦写下，每次重启都会先删表再建，运行期数据全部回到 data.sql 的初始状态。
-- 表已存在时 CREATE TABLE IF NOT EXISTS 什么都不做，数据原样保留；索引同理。
-- 示例数据不在这里导入，见 init/DataInitializer：仅当 resume_version 为空时执行一次 data.sql。

CREATE TABLE IF NOT EXISTS ai_chat_history (
  id bigint NOT NULL AUTO_INCREMENT,
  session_id varchar(64) DEFAULT NULL,
  question text,
  answer text,
  source varchar(20) DEFAULT 'local',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS education (
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

CREATE TABLE IF NOT EXISTS experience (
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

CREATE TABLE IF NOT EXISTS honor (
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

CREATE TABLE IF NOT EXISTS portfolio (
  id bigint NOT NULL AUTO_INCREMENT,
  owner_name varchar(50) NOT NULL,
  title varchar(100) DEFAULT NULL,
  cover varchar(255) DEFAULT NULL,
  url varchar(255) DEFAULT NULL,
  description text,
  sort int NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS profile (
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

CREATE TABLE IF NOT EXISTS resume_version (
  id bigint NOT NULL AUTO_INCREMENT,
  version_name varchar(100) NOT NULL,
  description varchar(500) DEFAULT NULL,
  is_default tinyint DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (is_default)
);

CREATE TABLE IF NOT EXISTS share_link (
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

CREATE TABLE IF NOT EXISTS site_config (
  id bigint NOT NULL,
  site_title varchar(100) DEFAULT '个人简历',
  default_theme varchar(50) DEFAULT 'default',
  ai_enabled tinyint NOT NULL DEFAULT 1,
  update_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS skill (
  id bigint NOT NULL AUTO_INCREMENT,
  version_id bigint NOT NULL DEFAULT 1,
  category varchar(50) DEFAULT NULL,
  name varchar(50) DEFAULT NULL,
  level int DEFAULT 80,
  sort int NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user (
  id bigint NOT NULL AUTO_INCREMENT,
  username varchar(50) NOT NULL,
  password varchar(100) NOT NULL,
  nickname varchar(50) DEFAULT NULL,
  avatar varchar(255) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS visit_log (
  id bigint NOT NULL AUTO_INCREMENT,
  ip varchar(64) DEFAULT NULL,
  user_agent varchar(500) DEFAULT NULL,
  referer varchar(500) DEFAULT NULL,
  path varchar(255) DEFAULT NULL,
  session_id varchar(64) DEFAULT NULL,
  visit_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_ai_session ON ai_chat_history(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_create_time ON ai_chat_history(create_time);
CREATE INDEX IF NOT EXISTS idx_education_version ON education(version_id);
CREATE INDEX IF NOT EXISTS idx_experience_version_type ON experience(version_id, type);
CREATE INDEX IF NOT EXISTS idx_honor_owner ON honor(owner_name);
CREATE INDEX IF NOT EXISTS idx_portfolio_owner ON portfolio(owner_name);
CREATE INDEX IF NOT EXISTS idx_profile_version ON profile(version_id);
CREATE INDEX IF NOT EXISTS idx_share_enabled ON share_link(enabled);
CREATE INDEX IF NOT EXISTS idx_skill_version ON skill(version_id);
CREATE INDEX IF NOT EXISTS idx_visit_time ON visit_log(visit_time);
CREATE INDEX IF NOT EXISTS idx_visit_ip ON visit_log(ip);