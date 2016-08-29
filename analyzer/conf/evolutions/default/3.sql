-- Since PostgreSQL can't convert points to polygons due to lack of conversion functions, just re-create all tables.

# --- !Ups

DROP TABLE IF EXISTS "underground_exits";

CREATE TABLE "underground_exits" (
  "id" SERiAL NOT NULL UNIQUE,
  "station_id" INT NOT NULL REFERENCES "underground_stations"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  "coordinates" GEOGRAPHY (POLYGON, 4326) NOT NULL,
  "description" CHARACTER VARYING(256)
);

DROP TABLE IF EXISTS "ground_routes_stops";
DROP TABLE IF EXISTS "ground_stops";

CREATE TABLE "ground_stops" (
  "id" SERIAL NOT NULL PRIMARY KEY,
  "name" CHARACTER VARYING (256) NOT NULL,
  "coordinates" GEOGRAPHY (POLYGON, 4326) NOT NULL
);

CREATE TABLE "ground_routes_stops" (
  "id" SERIAL NOT NULL PRIMARY KEY,
  "route_id" INTEGER REFERENCES "ground_routes"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  "stop_id" INTEGER REFERENCES "ground_stops"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  "forward" BOOLEAN NOT NULL
);

CREATE INDEX "ground_stops_coordinates_idx" ON "ground_stops" USING GIST("coordinates");

# --- !Downs

DROP TABLE IF EXISTS "underground_exits";

CREATE TABLE "underground_exits" (
  "id" SERiAL NOT NULL UNIQUE,
  "station_id" INT NOT NULL REFERENCES "underground_stations"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  "coordinates" GEOGRAPHY (POINT, 4326) NOT NULL,
  "description" CHARACTER VARYING(256)
);

DROP TABLE IF EXISTS "ground_routes_stops";
DROP TABLE IF EXISTS "ground_stops";

CREATE TABLE "ground_stops" (
  "id" SERIAL NOT NULL PRIMARY KEY,
  "name" CHARACTER VARYING (256) NOT NULL,
  "coordinates" GEOGRAPHY (POINT, 4326) NOT NULL
);

CREATE TABLE "ground_routes_stops" (
  "id" SERIAL NOT NULL PRIMARY KEY,
  "route_id" INTEGER REFERENCES "ground_routes"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  "stop_id" INTEGER REFERENCES "ground_stops"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  "forward" BOOLEAN NOT NULL
);

CREATE INDEX "ground_stops_coordinates_idx" ON "ground_stops" USING GIST("coordinates");