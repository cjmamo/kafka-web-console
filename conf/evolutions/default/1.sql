# --- !Ups

CREATE TABLE servers (
  address VARCHAR,
  port INT,
  statusId LONG,
  groupId LONG,
  PRIMARY KEY (address, port)
);

CREATE TABLE groups (
  id LONG,
  name VARCHAR,
  PRIMARY KEY (id)
);

CREATE TABLE status (
  id LONG,
  name VARCHAR,
  PRIMARY KEY (id)
);

INSERT INTO groups (id, name) VALUES (0, 'ALL');

INSERT INTO status (id, name) VALUES (0, 'DISCONNECTED');
INSERT INTO status (id, name) VALUES (1, 'CONNECTED');

# --- !Downs

DROP TABLE IF EXISTS servers;
DROP TABLE IF EXISTS groups;
DROP TABLE IF EXISTS status;