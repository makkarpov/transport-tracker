Transport tracker
=================

**Abandoned and not developed anymore**

An unfinished attempt to implement automatical detection and suggestion of most effective public transport routes. Currently constists of following components:
* *tracker:* an Android application that collects data about user transportation and send it to server for analysis:
  * Collects and stores GPS tracks (a sequence of points containing time, latitude, longitude, altitude, speed, bearing and accelerometer data)
  * Analyzes accelerometer data to detect current movement type (currently implemented detection only for pedestrian movements)
  * Autostarts and autostops track recording based on current Wi-Fi networks (e.g. starts recording when home network is lost and stops when office network appears)
  * Uploads recorded tracks to server (see *analyzer*) and fetches analyzed track variant (a track that holds metadata for all points about which transporation was used at this point --- e.g. pedestrian, or underground from station A to station B, or bus route #N from station C to station D etc.)
  * Based on track and analysis calculates some statistics about transportation routes (average trip time, average wait time, ...)
  * Calculates some basic statistics about pedestrian movement (total distance walked, average walking speed, walking speed by day).
* *analyzer*: an Play! Framework application backed by Postgres database that holds routes, underground stations, bus stops and analyzes received tracks based on this data.

Project was not finished due to lack of time to solve these problems:
* Analysis of received tracks was much more complicated problem than I initially expected, since analysis data is a basis for all further statistics, poor analyze algorithms renders the whole application completely unusable
* Lack of Android development experience caused pretty poor Android code, so some refactoring is needed by someone with sufficient development experience.
