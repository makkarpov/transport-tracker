package ru.makkarpov.ttdroid.settings

import ru.makkarpov.ttdroid.R

/**
  * Created by user on 8/1/16.
  */
class TrackingPreferenceFragment extends BasePreferenceFragment(R.xml.pref_tracking) {
  override val preferenceListened = Seq("api_url")
}
