package ru.makkarpov.ttanalyze.analyze

import play.api.libs.json.Json
import play.twirl.api.Html
import ru.makkarpov.ttanalyze.analyze.TrackFragment.FragmentMetadata

/* Since metadata could be calculated independently from other logic (based on raw points and start/stop of fragments,
 * it is kept in separate array to avoid mixing it with fragment calculation logic.
 */
case class AnalyzedTrack(rawPoints: Seq[TrackPoint], fragments: Seq[TrackFragment], fragmentMetas: Seq[FragmentMetadata],
                         hash: String) {
  def json = Json.obj(
    "raw" -> rawPoints.map(TrackPoint.jsonWrites.writes),
    "analyzed" -> fragments.indices.map(i => TrackFragment.writeFrament(fragments(i), fragmentMetas(i)))
  )

  def apiJson = Json.obj(
    "fileHash" -> hash,
    "fragments" -> fragments.indices.map(i => TrackFragment.writeFrament(fragments(i), fragmentMetas(i)))
  )

  def jsonStr = Html(json.toString())
}
