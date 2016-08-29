package ru.makkarpov.ttdroid

import java.io.{File, IOException}

import android.app.{ProgressDialog, AlertDialog}
import android.content.DialogInterface.OnClickListener
import android.content.{DialogInterface, Intent}
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import android.view._
import android.widget.AdapterView.OnItemClickListener
import android.widget._
import ru.makkarpov.ttdroid.FileListActivity.ListedFile
import ru.makkarpov.ttdroid.data.TrackFiles
import ru.makkarpov.ttdroid.map.MapActivity
import ru.makkarpov.ttdroid.utils.Extensions._
import ru.makkarpov.ttdroid.utils.Utils._

object FileListActivity {
  case class ListedFile(file: File, time: Long, points: Int, finished: Boolean, analyzed: Boolean)

  def allFiles: Array[TrackFiles] =
    Option(TTDroid.tracksDirectory.listFiles())
      .getOrElse(Array.empty)
      .filter(_.getName.startsWith("t_"))
      .map(new TrackFiles(_))
}

class FileListActivity extends BaseActivity(R.layout.activity_file_list) { act =>
  var files = rescanFiles()
  lazy val filesList = findViewById(R.id.fileList).asInstanceOf[ListView]

  protected override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    filesList.setOnItemClickListener(new OnItemClickListener {
      override def onItemClick(adapterView: AdapterView[_], view: View, i: Int, l: Long): Unit =
        openFile(files(i).file)
    })

    filesList.setOnCreateContextMenuListener(new OnCreateContextMenuListener {
      override def onCreateContextMenu(menu: ContextMenu, view: View,
                                       menuInfo: ContextMenuInfo): Unit = {

        val listInfo = menuInfo.asInstanceOf[AdapterView.AdapterContextMenuInfo]
        val file = files(listInfo.position)

        menu.setHeaderTitle(dateStr(file.time))
        getMenuInflater.inflate(R.menu.file_list_popup, menu)
      }
    })

    refreshList()
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    menu.clear()
    getMenuInflater.inflate(R.menu.file_list, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.reanalyzeAll =>
        reanalyzeAll()
        true

      case _ => super.onOptionsItemSelected(item)
    }
  }

  override def onContextItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.open | R.id.delete => // track menu
        val info = item.getMenuInfo.asInstanceOf[AdapterView.AdapterContextMenuInfo]
        val f = files(info.position)

        item.getItemId match {
          case R.id.open => openFile(f.file)
          case R.id.delete =>
            val msg = getString(R.string.fl_track_date) format (dateStr(f.time),
              quantityFmt(this, R.plurals.n_points, f.points))

            val dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.fl_confirm_deletion)
                .setMessage(msg)
                .setPositiveButton(R.string.fl_delete_yes, new OnClickListener {
                  override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
                    deleteFile(f.file)
                  }
                })
                .setNegativeButton(R.string.fl_delete_no, null)
                .create()

            dialog.show()
        }

        true

      case _ =>
        super.onContextItemSelected(item)
    }
  }

  private def reanalyzeAll(): Unit = {
    val files = FileListActivity.allFiles.filter(_.header.analyze.isDefined)
    Log.i("FileListActivity", s"${files.length} files to analyze")
    val dialog = new ProgressDialog(this)
    dialog.setTitle(R.string.fl_reanalyze_running)
    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    dialog.setMax(files.length)
    dialog.show()

    new Thread {
      setName("Reanalyze thread")
      setDaemon(true)
      start()

      override def run(): Unit = {
        for ((f, i) <- files.zipWithIndex) {
          Log.i("FileListActivity", s"Analyzing file #$i (${f.headerFile.getCanonicalPath})...")
          val resp = HttpApi.syncRequest(act, f)

          runOnUiThread(new Runnable {
            override def run(): Unit = {
              resp match {
                case Left(exception) =>
                  Toast.makeText(act, exception.toString, Toast.LENGTH_SHORT).show()

                case Right(data) => f.header = f.header.copy(analyze = Some(data))
              }

              dialog.setProgress(i + 1)
            }
          })
        }

        runOnUiThread(new Runnable {
          override def run(): Unit = {
            dialog.hide()
            Toast.makeText(act, R.string.fl_reanalyze_completed, Toast.LENGTH_LONG).show()
          }
        })
      }
    }
  }

  private def deleteFile(f: File): Unit = {
    Log.i("FileListActivity", s"Deleting file '${f.getCanonicalPath}'")

    try f.deleteDirectory() catch {
      case e: IOException =>
        e.printStackTrace()
        Log.w("FileListActivity", s"Failed to delete '${f.getCanonicalPath}'")
    }

    files = rescanFiles()
    refreshList()
  }

  private def openFile(f: File): Unit = {
    val open = new Intent(FileListActivity.this, classOf[MapActivity])
    open.putExtra("display_file", f.getCanonicalPath)
    startActivity(open)
  }

  private def refreshList(): Unit = {
    val adapter = new ArrayAdapter[ListedFile](this, android.R.layout.simple_list_item_2, files) {
      val inflater = getLayoutInflater

      override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
        val ret = inflater.inflate(R.layout.two_line_list_item, null)
        val file = files(position)

        val text1 = dateStr(file.time)
        val text2 = new StringBuilder
        text2 ++= quantityFmt(FileListActivity.this, R.plurals.n_points, file.points)
        text2 += ' '
        if (!file.finished) text2 ++= getString(R.string.fl_not_completed)
        else if (!file.analyzed) text2 ++= getString(R.string.fl_not_analyzed)

        ret.findViewById(android.R.id.text1).asInstanceOf[TextView].setText(text1)
        ret.findViewById(android.R.id.text2).asInstanceOf[TextView].setText(text2.result())

        ret
      }
    }

    filesList.setAdapter(adapter)
  }

  private def rescanFiles(): Array[ListedFile] =
    FileListActivity.allFiles
        .map(x => ListedFile(x.root, x.header.startTime, x.pointCount,
                             x.header.finishTime.isDefined, x.header.analyze.isDefined))
        .sortBy(-_.time)
}