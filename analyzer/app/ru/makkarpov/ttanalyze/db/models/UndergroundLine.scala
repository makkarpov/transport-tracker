package ru.makkarpov.ttanalyze.db.models

/**
  * Created by user on 7/14/16.
  */
case class UndergroundLine(id: Int, code: String, color: Int, textColor: Int, name: String) {
  def colorHex = "#%06X" format color
  def textColorHex = "#%06X" format textColor
}

object UndergroundLine {
  import ru.makkarpov.ttanalyze.db.PgDriver.api
  import api._

  class Table(t: Tag) extends api.Table[UndergroundLine](t, "underground_lines") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    def code = column[String]("code")
    def color = column[Int]("color")
    def textColor = column[Int]("text_color")
    def name = column[String]("name")

    def * = (id, code, color, textColor, name) <>
            ((UndergroundLine.apply _).tupled, UndergroundLine.unapply)
  }

  val query = TableQuery[Table]
}
