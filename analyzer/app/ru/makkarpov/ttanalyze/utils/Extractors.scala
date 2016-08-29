package ru.makkarpov.ttanalyze.utils

import javax.xml.bind.DatatypeConverter

import scala.util.Try

/**
  * Created by user on 8/11/16.
  */
object Extractors {
  object base64 {
    def apply(b: Array[Byte]): String = DatatypeConverter.printBase64Binary(b)
    def unapply(s: String): Option[Array[Byte]] = Try(DatatypeConverter.parseBase64Binary(s)).toOption
  }
}
