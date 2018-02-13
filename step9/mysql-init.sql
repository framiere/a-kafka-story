CREATE DATABASE IF NOT EXISTS db;

USE db;

CREATE TABLE IF NOT EXISTS application (
  id            INT          NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(255) NOT NULL,
  team_email    VARCHAR(255) NOT NULL,
  last_modified DATETIME     NOT NULL
);


INSERT INTO application (
  name,
  team_email,
  last_modified
) VALUES (
  'kafka',
  'kafka@team.co',
  NOW()
);
