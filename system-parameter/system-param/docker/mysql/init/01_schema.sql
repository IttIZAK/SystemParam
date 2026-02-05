CREATE TABLE IF NOT EXISTS system_param_tag (
  tag_code        VARCHAR(100) PRIMARY KEY,
  tag_name        VARCHAR(255) NOT NULL,
  tag_description TEXT NULL,
  tag_priority    INT NOT NULL DEFAULT 999
);

CREATE TABLE IF NOT EXISTS system_param (
  param_key      VARCHAR(100) PRIMARY KEY,
  param_value    TEXT NOT NULL,
  description    TEXT NULL,
  data_type      VARCHAR(20) NOT NULL,
  tag_code       VARCHAR(100) NULL,
  display_order  INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_system_param_tag
    FOREIGN KEY (tag_code) REFERENCES system_param_tag(tag_code)
);
