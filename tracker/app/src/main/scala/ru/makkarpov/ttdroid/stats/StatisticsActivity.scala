package ru.makkarpov.ttdroid.stats

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.support.v7.widget.ThemedSpinnerAdapter
import android.view.{View, ViewGroup}
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.{AdapterView, ArrayAdapter, Spinner, TextView}
import ru.makkarpov.ttdroid.stats.StatisticsActivity.MyAdapter
import ru.makkarpov.ttdroid.{BaseActivity, R}

object StatisticsActivity {
  private class MyAdapter(context: Context, objects: Array[String])
  extends ArrayAdapter[String](context, android.R.layout.simple_list_item_1, objects)
  with ThemedSpinnerAdapter {
    private val mDropDownHelper = new ThemedSpinnerAdapter.Helper(context)

    override def getDropDownView(position: Int, convertView: View, parent: ViewGroup): View = {
      val view: View = convertView match {
        case null =>
          val inflater = mDropDownHelper.getDropDownViewInflater
          inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        case x => x
      }

      val textView = view.findViewById(android.R.id.text1).asInstanceOf[TextView]
      textView.setText(getItem(position))
      view
    }

    override def getDropDownViewTheme = mDropDownHelper.getDropDownViewTheme

    override def setDropDownViewTheme(theme: Resources#Theme): Unit =
      mDropDownHelper.setDropDownViewTheme(theme)
  }
}

class StatisticsActivity extends BaseActivity(R.layout.activity_statistics) {
  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    getSupportActionBar.setDisplayShowTitleEnabled(false)

    val spinner = findViewById(R.id.spinner).asInstanceOf[Spinner]
    spinner.setAdapter(new MyAdapter(toolbar.getContext, getResources.getStringArray(R.array.stats_tabs)))
    spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      override def onItemSelected(parent: AdapterView[_], view: View, position: Int, id: Long): Unit = {
        val frag = position match {
          case 0 => new InGroupFragment(false)
          case 1 => new InGroupFragment(true)
          case 2 => new PedestrianFragment
        }

        getSupportFragmentManager.beginTransaction()
          .replace(R.id.container, frag)
          .commit()
      }

      override def onNothingSelected(parent: AdapterView[_]): Unit = {}
    })
  }
}