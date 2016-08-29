package ru.makkarpov.ttdroid.settings

import java.nio.charset.StandardCharsets

import android.content.{BroadcastReceiver, Context, Intent, IntentFilter}
import android.net.wifi.{WifiConfiguration, WifiManager}
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget._
import ru.makkarpov.ttdroid.R
import ru.makkarpov.ttdroid.data.MyPlace
import ru.makkarpov.ttdroid.data.MyPlace.WiFiEntry
import ru.makkarpov.ttdroid.settings.EditPlaceActivity._
import ru.makkarpov.ttdroid.utils.Extensions._

import scala.collection.JavaConversions._

object EditPlaceActivity {
  val ResultOk = 0xFACE

  class NetworksListAdapter(ref: EditPlaceActivity) extends BaseAdapter {
    val inflater = ref.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]

    override def getItemId(i: Int): Long = i

    override def getCount: Int = ref.scannedNetworks.size
    override def getItem(i: Int): WiFiEntry = ref.scannedNetworks(i)

    override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val view = inflater.inflate(R.layout.checkbox_list_item, null)
      val elem = getItem(position)

      val cb = view.findViewById(R.id.checkBox).asInstanceOf[CheckBox]
      cb.setChecked(ref.selectedNetworks.contains(elem))
      cb.setOnCheckedChangeListener(new OnCheckedChangeListener {
        override def onCheckedChanged(compoundButton: CompoundButton, b: Boolean): Unit = {
          ref.checkedChange(elem, b)
        }
      })

      cb.setText(elem.ssid)

      view.findViewById(R.id.text).asInstanceOf[TextView].setText("")

      view
    }
  }
}

class EditPlaceActivity extends AppCompatActivity {
  lazy val name = findViewById(R.id.nameField).asInstanceOf[EditText]
  lazy val networks = findViewById(R.id.networksField).asInstanceOf[EditText]
  lazy val networksList = findViewById(R.id.networksList).asInstanceOf[ListView]

  lazy val wifiManager = getSystemService(Context.WIFI_SERVICE).asInstanceOf[WifiManager]
  var scannedNetworks = Seq.empty[WiFiEntry]
  var selectedNetworks = Set.empty[WiFiEntry]

  val wifiReceiver = new BroadcastReceiver {
    override def onReceive(context: Context, intent: Intent): Unit = {
      refreshNetworks()
    }
  }

  protected override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.my_places_edit)

    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    val loc = getIntent.extra[MyPlace]("location")
    name.setText(loc.name)

    selectedNetworks = loc.networks
    networksList.setAdapter(new NetworksListAdapter(this))

    refreshNetworks()
    registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

    findViewById(R.id.saveButton).setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        val ret = MyPlace(loc.id, name.getText.toString, selectedNetworks)
        val data = new Intent()
        data.putExtra("location", ret)
        setResult(ResultOk, data)
        finish()
      }
    })
  }

  override def onDestroy(): Unit = {
    unregisterReceiver(wifiReceiver)
    super.onDestroy()
  }

  private def checkedChange(e: WiFiEntry, sel: Boolean): Unit = {
    if (sel) selectedNetworks += e
    else selectedNetworks -= e
    refreshNetworks()
  }

  private def refreshNetworks(): Unit = {
    def convertConfigured(s: WifiConfiguration): WiFiEntry = {
      var ssid = s.SSID

      if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
        ssid = ssid.substring(1, ssid.length - 1)
      } else {
        // SSID is hex encoded
        val bytes = ssid.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray
        ssid = new String(bytes, StandardCharsets.UTF_8)
      }

      WiFiEntry(ssid)
    }

    scannedNetworks = (
      wifiManager.getScanResults.map(x => WiFiEntry(x.SSID)) ++
      wifiManager.getConfiguredNetworks.map(convertConfigured)
    ).filterNot(_.ssid.isEmpty).distinct

    networks.setText(selectedNetworks.map(_.ssid).mkString(", "))
    networksList.getAdapter.asInstanceOf[NetworksListAdapter].notifyDataSetChanged()
  }
}