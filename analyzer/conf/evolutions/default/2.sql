# --- !Ups

CREATE TYPE "ground_transport" AS ENUM ('Bus', 'Trolleybus', 'Tram', 'Monorail', 'SuburbanTrain');

CREATE TABLE "ground_routes" (
  "id" SERIAL NOT NULL PRIMARY KEY,
  "index" CHARACTER VARYING (64) NOT NULL,
  "type" "ground_transport" NOT NULL,
  "forward_path" GEOGRAPHY (LINESTRING, 4326) NOT NULL,
  "backward_path" GEOGRAPHY (LINESTRING, 4326) NOT NULL
);

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

CREATE INDEX "ground_routes_fwd_path_idx" ON "ground_routes" USING GIST("forward_path");
CREATE INDEX "ground_routes_bwd_path_idx" ON "ground_routes" USING GIST("backward_path");
CREATE INDEX "ground_stops_coordinates_idx" ON "ground_stops" USING GIST("coordinates");

# --- !Downs

DROP INDEX "ground_stops_coordinates_idx";
DROP INDEX "ground_routes_bwd_path_idx";
DROP INDEX "ground_routes_fwd_path_idx";

DROP TABLE "ground_routes_stops";
DROP TABLE "ground_stops";
DROP TABLE "ground_routes";

DROP TYPE "ground_transport";