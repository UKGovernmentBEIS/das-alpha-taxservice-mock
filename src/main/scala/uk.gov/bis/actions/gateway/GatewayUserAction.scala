package uk.gov.bis.actions.gateway

import com.google.inject.Inject
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.bis.db.gateway._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GatewayIdRequest[A](val request: Request[A], val ggId: GatewayIdRow) extends WrappedRequest[A](request)

class GatewayUserAction @Inject()(gatewayIds: GatewayIdDAO)(implicit ec: ExecutionContext)
  extends ActionBuilder[GatewayIdRequest]
    with ActionRefiner[Request, GatewayIdRequest] {

  val sessionKey = "mtdpId"

  val continueKey = "continue"

  override protected def refine[A](request: Request[A]): Future[Either[Result, GatewayIdRequest[A]]] = {
    implicit val rh: RequestHeader = request
    val redirectToSignIn = Left(Redirect(uk.gov.bis.controllers.gateway.routes.GatewaySignInController.showSignIn()).addingToSession(continueKey -> request.uri))

    request.session.get(sessionKey) match {
      case None => Future.successful(redirectToSignIn)
      case Some(id) => gatewayIds.byId(id).map {
        case Some(u) => Right(new GatewayIdRequest(request, u))
        case None => redirectToSignIn
      }
    }
  }
}

object ParseLong {
  def unapply(s: String): Option[Long] = Try(s.toLong).toOption
}
