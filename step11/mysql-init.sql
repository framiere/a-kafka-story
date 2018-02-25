GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'replicator' IDENTIFIED BY 'replpass';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT  ON *.* TO 'debezium' IDENTIFIED BY 'dbz';


CREATE DATABASE IF NOT EXISTS db;
GRANT ALL PRIVILEGES ON db.* TO 'mysqluser'@'%';

USE db;

CREATE TABLE IF NOT EXISTS application (
  id            INT          NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(255) NOT NULL,
  team_email    VARCHAR(255) NOT NULL,
  last_modified DATETIME     NOT NULL
);


INSERT INTO application (
  id,
  name,
  team_email,
  last_modified
) VALUES (
  1,
  'kafka',
  'kafka@apache.org',
  NOW()
);
