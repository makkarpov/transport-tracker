package ru.makkarpov.ttdroid

import android.content.{SharedPreferences, Intent, Context, BroadcastReceiver}
import android.preference.PreferenceManager
import android.util.Log
import ru.makkarpov.ttdroid.settings.SettingsActivity

/**
  * Created by user on 8/1/16.
  */
class GlobalActionsReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent): Unit = {
    Log.i("GlobalActionsReceiver", s"Received ${intent.getAction}")

    intent.getAction match {
      case Intent.ACTION_BOOT_COMPLETED =>
        refreshPlaceService(context)

      case SettingsActivity.SettingsChangedAction =>
        refreshPlaceService(context)
    }
  }

  private def refreshPlaceService(ctx: Context): Unit = {
    val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
    if (prefs.getBoolean("auto_start", true)) {
      Log.i("GlobalActionsReceiver", "Starting place watch service as auto-start is enabled")
      ctx.startService(new Intent(ctx, classOf[PlaceWatchService]))
    }
  }
}
