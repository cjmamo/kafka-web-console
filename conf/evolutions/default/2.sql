# --- !Ups

ALTER TABLE zookeepers ADD COLUMN chroot VARCHAR;

# --- !Downs

ALTER TABLE zookeepers DROP COLUMN chroot;