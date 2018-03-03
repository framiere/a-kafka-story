GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'replicator' IDENTIFIED BY 'replpass';

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT  ON *.* TO 'debezium' IDENTIFIED BY 'dbz';


CREATE DATABASE mydb;

GRANT ALL PRIVILEGES ON mydb.* TO 'user'@'%';

USE mydb;

CREATE TABLE team (
  id            INT          NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(255) NOT NULL,
  last_modified DATETIME     NOT NULL
);

INSERT INTO team (
  id,
  name,
  last_modified
) VALUES (
  '1',
  'kafka',
  NOW()
);

CREATE TABLE member (
  id       INT          NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name     VARCHAR(255) NOT NULL,
  team_id  INT          NOT NULL,
  FOREIGN KEY (team_id) REFERENCES team(id)
);

INSERT INTO member (
  id,
  name,
  team_id
) VALUES (
  '1',
  'jun rao',
  1
);

