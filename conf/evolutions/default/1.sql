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

INSERT INTO servers (address, port, groupId, statusId) VALUES ('localhost', 2181, 0, 0);
INSERT INTO servers (address, port, groupId, statusId) VALUES ('localhost2', 9999, 0, 0);

# --- !Downs

DROP TABLE IF EXISTS servers;
DROP TABLE IF EXISTS groups;