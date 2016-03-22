package actions.gateway

import com.google.inject.Inject
import db.gateway.{GatewayUserDAO, GatewayUserRow}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


class GatewayUserRequest[A](val request: Request[A], val user: GatewayUserRow) extends WrappedRequest[A](request)

class GatewayUserAction @Inject()(gatewayUsers: GatewayUserDAO)(implicit ec: ExecutionContext)
  extends ActionBuilder[GatewayUserRequest]
    with ActionRefiner[Request, GatewayUserRequest] {

  val sessionKey = "ggUserId"

  override protected def refine[A](request: Request[A]): Future[Either[Result, GatewayUserRequest[A]]] = {
    implicit val rh: RequestHeader = request
    val login = Left(Redirect(controllers.gateway.routes.GatewayLoginController.showLogin()).addingToSession("uri" -> request.uri))

    request.session.get(sessionKey) match {
      case None => Future.successful(login)
      case Some(ParseLong(id)) => gatewayUsers.byId(id).map {
        case Some(u) => Right(new GatewayUserRequest(request, u))
        case None => login
      }
      case Some(s) => Future.successful(login)
    }
  }
}

object ParseLong {
  def unapply(s: String): Option[Long] = Try(s.toLong).toOption
}
