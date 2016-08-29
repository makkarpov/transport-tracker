package ru.makkarpov.ttdroid.settings

import android.annotation.TargetApi
import android.app.{Activity, Fragment}
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.{SharedPreferences, Context, Intent}
import android.content.res.Configuration
import android.os.{Build, Bundle}
import android.preference.{PreferenceManager, PreferenceActivity, PreferenceFragment}
import android.support.v4.app.NavUtils
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.{LayoutInflater, MenuItem}
import android.widget.LinearLayout
import ru.makkarpov.ttdroid.R

object SettingsActivity {
  val SettingsChangedAction = "ru.makkarpov.ttdroid.settings.SETTINGS_CHANGED"

  val KnownFragments = Seq[Class[_]](
    classOf[MyPlacesFragment],
    classOf[TrackingPreferenceFragment]
  )

  def broadcastSettingsChanged(ctx: Context): Unit = {
    val intent = new Intent(SettingsChangedAction)
    ctx.sendBroadcast(intent)
    LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
  }
}

class SettingsActivity extends AppCompatPreferenceActivity with OnSharedPreferenceChangeListener {
  private var currentFragment: Fragment = _

  protected override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    val actionBar = getSupportActionBar
    if (actionBar != null)
      actionBar.setDisplayHomeAsUpEnabled(true)

    PreferenceManager.getDefaultSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(this)
  }

  override def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String): Unit = {
    SettingsActivity.broadcastSettingsChanged(this)
  }

  override def onAttachFragment(fragment: Fragment): Unit = {
    super.onAttachFragment(fragment)
    currentFragment = fragment
  }

  override def onMenuItemSelected(featureId: Int, item: MenuItem): Boolean =
    item.getItemId match {
      case android.R.id.home =>
        if (currentFragment != null) {
          NavUtils.navigateUpTo(this, new Intent(this, this.getClass))
          currentFragment = null
        } else if (!super.onMenuItemSelected(featureId, item))
          NavUtils.navigateUpFromSameTask(this)

        true

      case _ => super.onMenuItemSelected(featureId, item)
    }

  override def onIsMultiPane: Boolean =
    (getResources.getConfiguration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
      Configuration.SCREENLAYOUT_SIZE_XLARGE

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  override def onBuildHeaders(target: java.util.List[PreferenceActivity.Header]): Unit =
    loadHeadersFromResource(R.xml.pref_headers, target)

  protected override def isValidFragment(fragmentName: String): Boolean =
    SettingsActivity.KnownFragments.exists(_.getName == fragmentName)
}