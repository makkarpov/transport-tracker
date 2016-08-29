package ru.makkarpov.ttdroid.settings

import android.app.{AlertDialog, Fragment}
import android.content.DialogInterface.{OnClickListener => DOnClickListener}
import android.content.{DialogInterface, Intent}
import android.os.Bundle
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.{OnClickListener, OnCreateContextMenuListener}
import android.view._
import android.widget.AdapterView.OnItemClickListener
import android.widget.{AdapterView, ArrayAdapter, ListView, TextView}
import ru.makkarpov.ttdroid.R
import ru.makkarpov.ttdroid.data.MyPlace
import ru.makkarpov.ttdroid.utils.Extensions._

/**
  * Created by user on 7/29/16.
  */
object MyPlacesFragment {

}

class MyPlacesFragment extends Fragment {
  lazy val locationDb = new SettingsDatabase(getActivity)
  var placesList: ListView = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, state: Bundle): View = {
    val view = inflater.inflate(R.layout.my_places_list, null)

    view.findViewById(R.id.addButton).setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        openEditor(new MyPlace(-1, "", Set.empty))
      }
    })

    placesList = view.findViewById(R.id.knownPlaces).asInstanceOf[ListView]
    placesList.setOnItemClickListener(new OnItemClickListener {
      override def onItemClick(ad: AdapterView[_], v: View, pos: Int, l: Long): Unit = {
        openEditor(location(pos))
      }
    })

    placesList.setOnCreateContextMenuListener(new OnCreateContextMenuListener {
      override def onCreateContextMenu(menu: ContextMenu, view: View,
                                       menuInfo: ContextMenuInfo): Unit = {

        val listInfo = menuInfo.asInstanceOf[AdapterView.AdapterContextMenuInfo]
        val loc = location(listInfo.position)

        menu.setHeaderTitle(loc.name)
        getActivity.getMenuInflater.inflate(R.menu.mp_long_click, menu)
      }
    })

    rescanLocations()

    view
  }

  private def openEditor(e: MyPlace): Unit = {
    val intent = new Intent(MyPlacesFragment.this.getActivity, classOf[EditPlaceActivity])
    intent.putExtra("location", e)
    startActivityForResult(intent, 0)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    if (resultCode == EditPlaceActivity.ResultOk) {
      locationDb.storeLocation(data.extra[MyPlace]("location"))
      SettingsActivity.broadcastSettingsChanged(getActivity)
      rescanLocations()
    }
  }

  override def onContextItemSelected(item: MenuItem): Boolean = {
    val info = item.getMenuInfo.asInstanceOf[AdapterView.AdapterContextMenuInfo]
    val loc = location(info.position)

    item.getItemId match {
      case R.id.mp_menu_edit =>
        openEditor(loc)

      case R.id.mp_menu_remove =>
        val dialog = new AlertDialog.Builder(getActivity)
          .setTitle(R.string.place_delete_title)
          .setMessage(getActivity.getString(R.string.place_delete_message) format loc.name)
          .setPositiveButton(R.string.place_delete_yes, new DOnClickListener {
            override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
              locationDb.deleteLocation(loc)
              SettingsActivity.broadcastSettingsChanged(getActivity)
              rescanLocations()
            }
          })
          .setNegativeButton(R.string.place_delete_no, null)
          .create()

        dialog.show()
    }

    true
  }

  private def rescanLocations(): Unit = {
    val adapter = new ArrayAdapter[MyPlace](getActivity, 0, locationDb.getLocations.toArray) {
      val inflater = getActivity.getLayoutInflater

      override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
        val ret = inflater.inflate(R.layout.two_line_list_item, null)
        val itm = getItem(position)

        val text1 = itm.name match {
          case "" => s"#id: ${itm.id}"
          case x  => x
        }

        val text2 = itm.networks match {
          case Seq() => getResources.getString(R.string.mp_no_networks)
          case x => x.map(_.ssid).mkString(", ")
        }

        ret.findViewById(android.R.id.text1).asInstanceOf[TextView].setText(text1)
        ret.findViewById(android.R.id.text2).asInstanceOf[TextView].setText(text2)

        ret
      }
    }

    placesList.setAdapter(adapter)
  }

  private def location(i: Int) = placesList.getItemAtPosition(i).asInstanceOf[MyPlace]
}
