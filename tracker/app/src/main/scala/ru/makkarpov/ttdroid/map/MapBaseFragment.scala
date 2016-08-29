package ru.makkarpov.ttdroid.map

import android.app.Activity
import android.content.Context
import android.support.v4.app.Fragment

/**
  * Created by user on 8/3/16.
  */
class MapBaseFragment extends Fragment {
  var mapActivity: MapActivity = _

  override def onAttach(ctx: Context): Unit = {
    super.onAttach(ctx)
    setRetainInstance(true)
    mapActivity = getActivity.asInstanceOf[MapActivity]
  }

  protected def requestUpdate(): Unit =
    if (mapActivity != null)
      mapActivity.requestUpdate()

  protected def reloadSelf(): Unit =
    getFragmentManager.beginTransaction().detach(this).attach(this).commit()
}
