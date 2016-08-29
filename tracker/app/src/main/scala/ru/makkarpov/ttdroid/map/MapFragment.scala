package ru.makkarpov.ttdroid.map

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.TextView
import com.google.android.gms.maps.{SupportMapFragment, GoogleMap, OnMapReadyCallback}
import ru.makkarpov.ttdroid.R

/**
  * Created by user on 8/3/16.
  */
class MapFragment extends MapBaseFragment with OnMapReadyCallback {
  var statusText: TextView = _
  var map: GoogleMap = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    val ret = inflater.inflate(R.layout.main_tab_map, container, false)

    statusText = ret.findViewById(R.id.statusText).asInstanceOf[TextView]

    val mapFragment = SupportMapFragment.newInstance()
    val transaction = getChildFragmentManager.beginTransaction()
    transaction.add(R.id.map_container, mapFragment).commit()
    mapFragment.getMapAsync(this)

    ret
  }

  override def onMapReady(googleMap: GoogleMap): Unit = {
    map = googleMap
    requestUpdate()
  }
}