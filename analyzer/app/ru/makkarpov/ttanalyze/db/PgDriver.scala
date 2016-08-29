package ru.makkarpov.ttanalyze.db

import com.github.tminglei.slickpg._
import play.api.libs.json.{JsValue, Json}
import slick.ast.Library.SqlFunction
import slick.ast.TypedType
import slick.driver.JdbcProfile
import slick.jdbc.{GetResult, PositionedResult}
import slick.lifted.ExtensionMethods
import slick.profile.Capability

/**
  * Created by user on 7/14/16.
  */
object PgDriver extends ExPostgresDriver
  with PgArraySupport
  with PgDate2Support
  with PgRangeSupport
  with PgHStoreSupport
  with PgPlayJsonSupport
  with PgSearchSupport
  with PgPostGISSupport
  with PgNetSupport
  with PgLTreeSupport
  with PgEnumSupport
{
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
  super.computeCapabilities + JdbcProfile.capabilities.insertOrUpdate

  override val api = MyAPI

  val least = new SqlFunction("LEAST")
  val octetLength = new SqlFunction("OCTET_LENGTH")

  object MyAPI extends API with ArrayImplicits
    with DateTimeImplicits
    with JsonImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants
    with PostGISImplicits
  {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val playJsonArrayTypeMapper =
      new AdvancedArrayJdbcType[JsValue](pgjson,
        (s) => utils.SimpleArrayUtils.fromString[JsValue](Json.parse)(s).orNull,
        (v) => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)

    implicit val groundTransportTypeMapper = createEnumJdbcType("ground_transport", GroundTransport)
    // implicit val gtColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(GroundTransport)
    // implicit val gtOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder(GroundTransport)

    implicit class FloatExtensions(val c: Rep[Float]) extends ExtensionMethods[Float, Float] {
      override protected[this] implicit def b1Type = implicitly[TypedType[Float]]

      def min(b: Rep[Float])(implicit om: o#arg[Float, Float]#to[Float, Float]): Rep[Float] =
        om.column(least, n, b.toNode)
    }

    implicit class ByteaExtensions(val c: Rep[Array[Byte]]) extends ExtensionMethods[Array[Byte], Array[Byte]] {
      override protected[this] implicit def b1Type = implicitly[TypedType[Array[Byte]]]

      def length(implicit om: o#arg[Array[Byte], Array[Byte]]#to[Int, Int]): Rep[Int] =
        om.column(octetLength, n)
    }
  }
}

