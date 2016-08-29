package ru.makkarpov.ttdroid.settings

import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.annotation.{LayoutRes, Nullable}
import android.support.v7.app.{ActionBar, AppCompatDelegate}
import android.support.v7.widget.Toolbar
import android.view.{MenuInflater, View, ViewGroup}

/**
  * A {@link android.preference.PreferenceActivity} which implements and proxies the necessary calls
  * to be used with AppCompat.
  */
abstract class AppCompatPreferenceActivity extends PreferenceActivity {
  private var mDelegate: AppCompatDelegate = null

  protected override def onCreate(savedInstanceState: Bundle): Unit = {
    getDelegate.installViewFactory()
    getDelegate.onCreate(savedInstanceState)
    super.onCreate(savedInstanceState)
  }

  protected override def onPostCreate(savedInstanceState: Bundle): Unit = {
    super.onPostCreate(savedInstanceState)
    getDelegate.onPostCreate(savedInstanceState)
  }

  def getSupportActionBar: ActionBar = getDelegate.getSupportActionBar

  def setSupportActionBar(@Nullable toolbar: Toolbar): Unit = {
    getDelegate.setSupportActionBar(toolbar)
  }

  override def getMenuInflater: MenuInflater = getDelegate.getMenuInflater

  override def setContentView(@LayoutRes layoutResID: Int): Unit = {
    getDelegate.setContentView(layoutResID)
  }

  override def setContentView(view: View): Unit = getDelegate.setContentView(view)

  override def setContentView(view: View, params: ViewGroup.LayoutParams): Unit = {
    getDelegate.setContentView(view, params)
  }

  override def addContentView(view: View, params: ViewGroup.LayoutParams): Unit = {
    getDelegate.addContentView(view, params)
  }

  protected override def onPostResume(): Unit = {
    super.onPostResume()
    getDelegate.onPostResume()
  }

  protected override def onTitleChanged(title: CharSequence, color: Int): Unit = {
    super.onTitleChanged(title, color)
    getDelegate.setTitle(title)
  }

  override def onConfigurationChanged(newConfig: Configuration): Unit = {
    super.onConfigurationChanged(newConfig)
    getDelegate.onConfigurationChanged(newConfig)
  }

  protected override def onStop(): Unit = {
    super.onStop()
    getDelegate.onStop()
  }

  protected override def onDestroy(): Unit = {
    super.onDestroy()
    getDelegate.onDestroy()
  }

  override def invalidateOptionsMenu(): Unit = getDelegate.invalidateOptionsMenu()

  private def getDelegate: AppCompatDelegate = {
    if (mDelegate == null) {
      mDelegate = AppCompatDelegate.create(this, null)
    }

    mDelegate
  }
}