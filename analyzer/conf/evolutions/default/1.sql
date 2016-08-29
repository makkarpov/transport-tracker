-- Creates an underground stations tables

# --- !Ups

CREATE TABLE "underground_lines" (
  "id" SERIAL NOT NULL UNIQUE,
  "code" CHARACTER VARYING (8) NOT NULL,
  "color" INTEGER NOT NULL,
  "text_color" INTEGER NOT NULL,
  "name" CHARACTER VARYING (256) NOT NULL
);

CREATE TABLE "underground_stations" (
  "id" SERIAL NOT NULL UNIQUE,
  "line_id" INTEGER NOT NULL REFERENCES "underground_lines"("id") ON DELETE RESTRICT ON UPDATE RESTRICT,
  "name" CHARACTER VARYING(256) NOT NULL
);

CREATE TABLE "underground_exits" (
  "id" SERiAL NOT NULL UNIQUE,
  "station_id" INT NOT NULL REFERENCES "underground_stations"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  "coordinates" GEOGRAPHY (POINT, 4326) NOT NULL,
  "description" CHARACTER VARYING(256)
);

CREATE INDEX "underground_exits_coordinates_idx" ON "underground_exits" USING GIST( "coordinates" );

# --- !Downs

DROP INDEX "underground_exits_coordinates_idx";
DROP TABLE "underground_exits";
DROP TABLE "underground_stations";
DROP TABLE "underground_lines";