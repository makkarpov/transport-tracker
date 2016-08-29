package ru.makkarpov.ttdroid

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.{NavigationView, FloatingActionButton}
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.{MenuItem, View}
import android.view.View.OnClickListener
import ru.makkarpov.ttdroid.map.MapActivity
import ru.makkarpov.ttdroid.settings.SettingsActivity
import ru.makkarpov.ttdroid.stats.StatisticsActivity

/**
  * Created by user on 7/28/16.
  */
trait DrawerActivity extends Activity with OnNavigationItemSelectedListener {
  lazy val fab = findViewById(R.id.fab).asInstanceOf[FloatingActionButton]
  lazy val drawer = findViewById(R.id.drawer_layout).asInstanceOf[DrawerLayout]
  lazy val toolbar = findViewById(R.id.toolbar).asInstanceOf[Toolbar]
  lazy val navigationView = findViewById(R.id.nav_view).asInstanceOf[NavigationView]

  def resId: Int

  def fabPressed(): Unit = {}

  def setSupportActionBar(t: Toolbar): Unit

  protected override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(resId)
    setSupportActionBar(toolbar)

    if (fab != null)
      fab.setOnClickListener(new OnClickListener {
        override def onClick(view: View): Unit = fabPressed()
      })


    val toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open,
      R.string.navigation_drawer_close)
    drawer.addDrawerListener(toggle)
    toggle.syncState()

    navigationView.setNavigationItemSelectedListener(this)
  }

  override def onBackPressed(): Unit = {
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START)
    } else {
      super.onBackPressed()
    }
  }

  def onNavigationItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.menu_current_track => startActivity(new Intent(this, classOf[MapActivity]))
      case R.id.menu_stored_tracks => startActivity(new Intent(this, classOf[FileListActivity]))
      case R.id.menu_settings => startActivity(new Intent(this, classOf[SettingsActivity]))
      case R.id.menu_statistics => startActivity(new Intent(this, classOf[StatisticsActivity]))
    }

    drawer.closeDrawer(GravityCompat.START)
    true
  }
}
