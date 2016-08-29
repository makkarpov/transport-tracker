package ru.makkarpov.ttdroid.settings

import android.content.Intent
import android.os.Bundle
import android.preference._
import android.util.Log
import android.view.MenuItem

/**
  * Created by user on 7/29/16.
  */
abstract class BasePreferenceFragment(resId: Int) extends PreferenceFragment {
  val preferenceListened = Seq[String]()

  private val preferenceListener = new Preference.OnPreferenceChangeListener() {
    def onPreferenceChange(preference: Preference, value: AnyRef): Boolean = {
      val stringValue: String = value.toString
      preference match {
        case listPreference: ListPreference =>
          val index: Int = listPreference.findIndexOfValue(stringValue)
          preference.setSummary(if (index >= 0) listPreference.getEntries()(index) else null)
        case _ =>
          preference.setSummary(stringValue)
      }

      true
    }
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    if (resId != 0)
      addPreferencesFromResource(resId)
    setHasOptionsMenu(true)

    for (s <- preferenceListened) {
      val pref = findPreference(s)
      pref.setOnPreferenceChangeListener(preferenceListener)
      preferenceListener.onPreferenceChange(pref,
        PreferenceManager.getDefaultSharedPreferences(pref.getContext).getString(pref.getKey, ""))
    }

  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        startActivity(new Intent(getActivity, classOf[SettingsActivity]))
        true

      case _ => super.onOptionsItemSelected(item)
    }
  }
}