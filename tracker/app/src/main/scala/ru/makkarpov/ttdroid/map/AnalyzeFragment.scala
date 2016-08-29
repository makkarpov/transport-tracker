package ru.makkarpov.ttdroid.map

import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{ExpandableListView, Button, ProgressBar, Toast}
import ru.makkarpov.ttdroid.{HttpApi, R}
import ru.makkarpov.ttdroid.data.{TrackFiles, AnalyzedTrack}

/**
  * Created by user on 8/4/16.
  */
class AnalyzeFragment extends MapBaseFragment {
  private var analyzed = Option.empty[AnalyzedTrack]
  private var autoAnalyze = false

  private var spinner: ProgressBar = _
  private var button: Button = _

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    analyzed = mapActivity.displayedTrack.currentFile.map(_.header).flatMap(_.analyze)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup,
                            savedInstanceState: Bundle): View = analyzed match {
    case Some(track) =>
      Log.i("Analyzed", track.toString)

      spinner = null
      button = null

      val ret = inflater.inflate(R.layout.main_tab_analyze, container, false)
      val fragments = ret.findViewById(R.id.fragmentsView).asInstanceOf[ExpandableListView]
      fragments.setAdapter(new AnalyzedListAdapter(track.fragments, mapActivity))
      ret

    case None =>
      val ret = inflater.inflate(R.layout.main_tab_not_analyzed, container, false)

      spinner = ret.findViewById(R.id.progressBar).asInstanceOf[ProgressBar]
      button = ret.findViewById(R.id.button).asInstanceOf[Button]

      button.setOnClickListener(new OnClickListener {
        override def onClick(view: View): Unit = {
          analyze()
        }
      })

      if (autoAnalyze)
        analyze()

      ret
  }

  def reanalyzeEnabled: Boolean = analyzed.isDefined

  def reanalyzePressed(): Unit = {
    analyzed = None
    autoAnalyze = true
    reloadSelf()
  }

  private def analyze(): Unit = {
    for (f <- mapActivity.displayedTrack.currentFile) {
      autoAnalyze = false
      button.setVisibility(View.GONE)
      spinner.setVisibility(View.VISIBLE)

      HttpApi.request(mapActivity, f) {
        case Left(thr) =>
          Log.e("AnalyzeFragment", "HTTP request failed", thr)
          Toast.makeText(mapActivity, mapActivity.getString(R.string.http_error) format thr, Toast.LENGTH_SHORT).show()
          button.setVisibility(View.VISIBLE)
          spinner.setVisibility(View.GONE)

        case Right(track) =>
          f.header = f.header.copy(analyze = Some(track))
          analyzed = Some(track)

          reloadSelf()
      }
    }
  }
}
