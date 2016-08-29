package ru.makkarpov.ttanalyze.utils

import java.text.SimpleDateFormat
import java.util.Date

/**
  * Created by user on 7/15/16.
  */
object ViewUtils {
  val MapKey = "AIzaSyDkl1dqFkLRMjaZ4W347C-y6QG073XhDpY"

  def dateToStr(time: Long): String = new SimpleDateFormat("HH:mm:ss, dd.MM.yyyy").format(new Date(time))
  def intervalToStr(time: Long): String = f"${time / 360000}%d:${(time / 60000) % 60}%02d:${(time / 1000) % 60}%02d"
}
