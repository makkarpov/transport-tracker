package controllers

import javax.inject.Inject

import play.api.mvc._
import Results._
import ru.makkarpov.ttanalyze.Context
import ru.makkarpov.ttanalyze.Context._
import ru.makkarpov.ttanalyze.db.models.User
import UserController._

import scala.concurrent.Future

/**
  * Created by user on 8/11/16.
  */
object UserController {
  val PermReadTracks = 1

  case class UserRequest[+A](req: Request[A], user: Option[User] = None) extends WrappedRequest[A](req) {
    def has(mask: Long) = user match {
      case None => false
      case Some(usr) => (usr.permissions & mask) != 0
    }
  }

  def fetchUser(implicit ctx: Context) = Action.andThen(new ActionTransformer[Request, UserRequest] {
    override protected def transform[A](request: Request[A]): Future[UserRequest[A]] =
      request.session.get("user.id") match {
        case None => Future.successful(UserRequest(request))
        case Some(uid) =>
          User.byId(uid.toInt) map {
            case None => UserRequest(request)
            case Some(usr) => UserRequest(request, Some(usr))
          }
      }
  })

  def withRights(l: Long)(implicit ctx: Context) = fetchUser.andThen(new ActionFilter[UserRequest] {
    override protected def filter[A](request: UserRequest[A]): Future[Option[Result]] = request match {
      case UserRequest(_, None) => Future.successful(Some(Forbidden(views.html.user.forbidden()(request))))
      case UserRequest(_, Some(usr)) if (usr.permissions & l) == 0 =>
        Future.successful(Some(Forbidden(views.html.user.forbidden()(request))))
      case _ => Future.successful(None)
    }
  })

  def anonymous(implicit ctx: Context) = fetchUser.andThen(new ActionFilter[UserRequest] {
    override protected def filter[A](request: UserRequest[A]): Future[Option[Result]] =
      Future.successful(if (request.user.isDefined) Some(Redirect(routes.HomeController.index())) else None)
  })
}

class UserController @Inject()(implicit ctx: Context) extends Controller {
  import ctx.db.api._

  def login = anonymous.apply { implicit rq => Ok(views.html.user.login(None)) }

  def doLogin = anonymous.async(parse.urlFormEncoded) { implicit rq =>
    val username = formField("username")
    val password = formField("password")

    User.login(username, password) map {
      case None => Ok(views.html.user.login(Some("Неверный логин или пароль!")))
      case Some(usr) =>
        Redirect(routes.HomeController.index())
          .withNewSession
          .withSession("user.id" -> usr.id.toString)
    }
  }

  def register = anonymous.apply { implicit rq => Ok(views.html.user.register(None)) }

  def doRegister = anonymous.async(parse.urlFormEncoded) { implicit rq =>
    val username  = formField("username")
    val password1 = formField("password1")
    val password2 = formField("password2")

    User.register(username, password1, password2) map {
      case Left(err) => Ok(views.html.user.register(Some(err)))
      case Right(_) => Redirect(routes.UserController.login())
    }
  }

  def logout = fetchUser.apply { Redirect(routes.HomeController.index()).withNewSession }

  private def formField(s: String)(implicit rq: Request[Map[String, Seq[String]]]): String =
    rq.body.get(s).flatMap(_.headOption).getOrElse("")
}
