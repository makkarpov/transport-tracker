package ru.makkarpov.ttdroid.data

import java.io.File

import ru.makkarpov.ttdroid.utils.Extensions
import Extensions._

/**
  * Created by user on 7/12/16.
  */
class TrackFiles(val root: File) {
  root.makeDirectory()

  val headerFile = root / "header.json"
  val pointsFile = root / "points.bin"

  private var _header: TrackHeader = null

  def header = {
    if (_header eq null)
      _header = TrackHeaderFile.read(headerFile)
    _header
  }

  def header_=(h: TrackHeader) = {
    _header = h
    TrackHeaderFile.write(headerFile, h)
  }

  def points = new TrackPointFile(pointsFile, "rw")
  def pointsReadOnly = new TrackPointFile(pointsFile, "r")

  def pointCount = pointsReadOnly use { p => p.length }

  def trackStart = pointsReadOnly use { p =>
    p.seek(0)
    p.read().time
  }

  def trackTime = pointsReadOnly use { p =>
    p.seek(0)
    val st = p.read().time
    p.seek(p.length - 1)
    p.read().time - st
  }
}
