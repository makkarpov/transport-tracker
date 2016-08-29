package ru.makkarpov.ttanalyze.db.models

import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, SecureRandom}

import ru.makkarpov.ttanalyze.Context
import ru.makkarpov.ttanalyze.Context._
import ru.makkarpov.ttanalyze.utils.Extractors._

import scala.concurrent.Future

/**
  * Created by user on 8/11/16.
  */
case class User(id: Int, username: String, password: String, permissions: Long) {
  def authenticate(pwd: String): Option[User] = password.split(" ") match {
    case Array("sha512", base64(salt), base64(hash)) =>
      val md = MessageDigest.getInstance("SHA-512")
      val h1 = md.digest(pwd.getBytes(StandardCharsets.UTF_8))
      md.update(salt)
      val dgst = md.digest(h1)

      if (dgst sameElements hash) Some(this) else None

    case _ => None
  }
}

object User {
  import ru.makkarpov.ttanalyze.db.PgDriver.api
  import api._

  class Table(t: Tag) extends api.Table[User](t, "users") {
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def username    = column[String]("username")
    def password    = column[String]("password")
    def permissions = column[Long]("permissions")

    def * = (id, username, password, permissions) <>
            ((User.apply _).tupled, User.unapply)
  }

  val query = TableQuery[Table]

  def login(username: String, password: String)(implicit ctx: Context): Future[Option[User]] =
    ctx.db(User.query.filter(_.username.toLowerCase === username.toLowerCase).result
      .map(_.headOption.flatMap(_.authenticate(password))))

  def byId(id: Int)(implicit ctx: Context): Future[Option[User]] =
    ctx.db(User.query.filter(_.id === id).result.headOption)

  def register(username: String, password1: String, password2: String)(implicit ctx: Context): Future[Either[String, Unit]] = {
    if (username.isEmpty)
      return Future.successful(Left("Имя пользователя не может быть пустым!"))

    if (password1 != password2)
      return Future.successful(Left("Пароли не совпадают!"))

    if (password1.length < 6)
      return Future.successful(Left("Пароль слишком короткий (меньше шести символов)"))

    ctx.db(User.query.filter(_.username.toLowerCase === username.toLowerCase).exists.result) flatMap {
      case true => Future.successful(Left("Пользователь с таким именем уже существует!"))
      case false => ctx.db(User.query += User(-1, username, hashPassword(password1), 0L)) map { _ => Right(Unit) }
    }
  }

  private val prng = SecureRandom.getInstance("SHA1PRNG")

  def hashPassword(pwd: String): String = {
    val md = MessageDigest.getInstance("SHA-512")
    val salt = new Array[Byte](32)
    prng.nextBytes(salt)

    val h1 = md.digest(pwd.getBytes(StandardCharsets.UTF_8))
    md.update(salt)
    val dgst = md.digest(h1)

    Array( "sha512", base64(salt), base64(dgst) ).mkString(" ")
  }
}