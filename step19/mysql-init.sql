GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'replicator' IDENTIFIED BY 'replpass';

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT  ON *.* TO 'debezium' IDENTIFIED BY 'dbz';

CREATE DATABASE mydb;

GRANT ALL PRIVILEGES ON mydb.* TO 'user'@'%';

USE mydb;

CREATE TABLE Member  (
  id            INT          NOT NULL PRIMARY KEY AUTO_INCREMENT,
  firstName     VARCHAR(255) NOT NULL,
  lastName      VARCHAR(255) NOT NULL,
  gender        VARCHAR(255) NOT NULL,
  phone         VARCHAR(255) NOT NULL,
  maritalStatus VARCHAR(255) NOT NULL,
  teamId        INT          NOT NULL,
  age           INT          NOT NULL,
  role          VARCHAR(255) NOT NULL
);

CREATE TABLE Address  (
  id            INT          NOT NULL PRIMARY KEY AUTO_INCREMENT,
  streetName    VARCHAR(255) NOT NULL,
  streetAddress VARCHAR(255) NOT NULL,
  city          VARCHAR(255) NOT NULL,
  state         VARCHAR(255) NOT NULL,
  country       VARCHAR(255) NOT NULL
);

CREATE TABLE Team (
  id       INT          NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name     VARCHAR(255) NOT NULL
);
