package ru.makkarpov.ttdroid.map

import java.io.File

import android.content.{BroadcastReceiver, Context, Intent, IntentFilter}
import android.os.Bundle
import android.support.design.widget.{FloatingActionButton, TabLayout}
import android.support.v4.app.{FragmentManager, FragmentPagerAdapter}
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.util.Log
import android.view.View.OnClickListener
import android.view._
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import ru.makkarpov.ttdroid.data.TrackHeader
import ru.makkarpov.ttdroid.map.MapActivity.Frontend
import ru.makkarpov.ttdroid.map.TrackOverlayProvider.ColorMode
import ru.makkarpov.ttdroid.settings.SettingsActivity
import ru.makkarpov.ttdroid.{BaseActivity, R, TrackingService}
import ru.makkarpov.ttdroid.utils.Extensions._

/**
  * Created by user on 8/2/16.
  */
object MapActivity {
  trait Frontend extends Context {
    def statusText: TextView
    def fab: FloatingActionButton
    def map: GoogleMap
    def colorMode: ColorMode

    def clearSummary(): Unit
    def showSummary(hdr: TrackHeader): Unit
  }
}

class MapActivity extends BaseActivity(R.layout.activity_main) with Frontend {
  lazy val viewPager = findViewById(R.id.container).asInstanceOf[ViewPager]
  var menuColor = R.id.menuColorSpeed

  var displayFile = ""
  lazy val displayedTrack = displayFile match {
    case null => new LiveDisplayedTrack(this)
    case x => new StoredDisplayedTrack(this, new File(x))
  }

  val mapFragment = new MapFragment
  val infoFragment = new InfoFragment
  val analyzeFragment = new AnalyzeFragment

  class MapPagerAdapter(fm: FragmentManager) extends FragmentPagerAdapter(fm) {
    override def getItem(position: Int) = position match {
      case 0 => mapFragment
      case 1 => infoFragment
      case 2 => analyzeFragment
    }

    def getCount = if (displayedTrack.canBeAnalyzed) 3 else 2

    override def getPageTitle(position: Int) = getResources.getStringArray(R.array.map_tabs)(position)
  }

  val trackUpdateReceiver = new BroadcastReceiver {
    override def onReceive(context: Context, intent: Intent): Unit =
      requestUpdate()
  }

  protected override def onCreate(state: Bundle): Unit = {
    super.onCreate(state)

    displayFile = Option(getIntent.getStringExtra("display_file"))
      .orElse(Option(state).map(_.getString("display_file")))
      .orNull

    viewPager.setAdapter(new MapPagerAdapter(getSupportFragmentManager))
    findViewById(R.id.tabs).asInstanceOf[TabLayout].setupWithViewPager(viewPager)

    viewPager.addOnPageChangeListener(new OnPageChangeListener {
      override def onPageScrollStateChanged(state: Int): Unit = {}
      override def onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int): Unit = {}
      override def onPageSelected(position: Int): Unit = {
        invalidateOptionsMenu()
      }
    })

    LocalBroadcastManager.getInstance(this).registerReceiver(trackUpdateReceiver,
      new IntentFilter(TrackingService.TRACK_UPDATE))

    // To make sure that services such as PlaceWatch will be started on application start.
    SettingsActivity.broadcastSettingsChanged(this)

    if (displayedTrack.fabVisible) {
      fab.setOnClickListener(new OnClickListener {
        override def onClick(view: View): Unit = displayedTrack.fabPressed()
      })
    } else fab.setVisibility(View.GONE)

    requestUpdate()
  }

  override def onSaveInstanceState(outState: Bundle): Unit = {
    super.onSaveInstanceState(outState)
    outState.putString("display_file", displayFile)
  }

  override def onDestroy(): Unit = {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(trackUpdateReceiver)
    super.onDestroy()
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    viewPager.getCurrentItem match {
      case 0 =>
        menu.clear()
        getMenuInflater.inflate(R.menu.main, menu)
        menu.findItem(menuColor).setChecked(true)
        true

      case 1 =>
        menu.clear()
        getMenuInflater.inflate(R.menu.main_details, menu)
        true

      case 2 =>
        menu.clear()
        getMenuInflater.inflate(R.menu.main_analyze, menu)
        true

      case _ => false
    }
  }

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    Option(menu.findItem(R.id.zoomToTrack)).foreach(_.setEnabled(displayedTrack.zoomEnabled))
    Option(menu.findItem(R.id.reanalyzeTrack)).foreach(_.setEnabled(analyzeFragment.reanalyzeEnabled))

    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case R.id.menuColorSpeed | R.id.menuColorMovement =>
      menuColor = item.getItemId
      item.setChecked(true)
      requestUpdate()
      true

    case R.id.zoomToTrack if displayedTrack.zoomEnabled =>
      displayedTrack.zoomPressed()
      true

    case R.id.reanalyzeTrack if analyzeFragment.reanalyzeEnabled =>
      analyzeFragment.reanalyzePressed()
      true

    case R.id.setupPlaces =>
      infoFragment.setPlacesPressed()
      true

    case _ => false
  }

  def requestUpdate(): Unit = {
    if (mapFragment.statusText == null)
      return

    displayedTrack.updateRequested()
  }

  // Frontend:

  override def statusText = mapFragment.statusText
  override def map: GoogleMap = mapFragment.map
  override def colorMode: ColorMode = menuColor match {
    case R.id.menuColorSpeed => ColorMode.Speed
    case R.id.menuColorMovement => ColorMode.Movement
  }

  override def clearSummary(): Unit = infoFragment.clearSummary()
  override def showSummary(hdr: TrackHeader): Unit =
    infoFragment.showSummary(hdr)
}
