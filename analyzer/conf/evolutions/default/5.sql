# --- !Ups

CREATE TABLE "users" (
  "id" SERIAL NOT NULL PRIMARY KEY,
  "username" VARCHAR(256) NOT NULL,
  "password" VARCHAR(256) NOT NULL,
  "permissions" BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX "users_unique_username_idx" ON "users" USING BTREE(LOWER("username"));

# --- !Downs

DROP INDEX "users_unique_username_idx";
DROP TABLE "users";