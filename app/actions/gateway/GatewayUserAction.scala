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

  val sessionKey = "mtdpId"

  val continueKey = "continue"

  override protected def refine[A](request: Request[A]): Future[Either[Result, GatewayUserRequest[A]]] = {
    implicit val rh: RequestHeader = request
    val redirectToSignIn = Left(Redirect(controllers.gateway.routes.GatewaySignInController.showSignIn()).addingToSession(continueKey -> request.uri))

    request.session.get(sessionKey) match {
      case None => Future.successful(redirectToSignIn)
      case Some(ParseLong(id)) => gatewayUsers.byId(id).map {
        case Some(u) => Right(new GatewayUserRequest(request, u))
        case None => redirectToSignIn
      }
      case Some(s) => Future.successful(redirectToSignIn)
    }
  }
}

object ParseLong {
  def unapply(s: String): Option[Long] = Try(s.toLong).toOption
}
