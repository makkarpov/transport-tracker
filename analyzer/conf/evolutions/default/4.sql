# --- !Ups

CREATE TABLE "submitted_tracks" (
  "id" SERIAL NOT NULL PRIMARY KEY,
  "remote_addr" INET NOT NULL,
  "version" INTEGER NOT NULL,
  "sha256" VARCHAR(64) NOT NULL UNIQUE,
  "track_data" BYTEA NOT NULL,
  "submitted_at" TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

# --- !Downs

DROP TABLE "submitted_tracks";