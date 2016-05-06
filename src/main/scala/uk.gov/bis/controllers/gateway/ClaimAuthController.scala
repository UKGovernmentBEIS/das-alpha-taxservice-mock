package uk.gov.bis.controllers.gateway

import javax.inject.{Inject, Singleton}

import play.api.mvc.{AnyContent, Controller}
import uk.gov.bis.actions.gateway.{GatewayIdRequest, GatewayUserAction}
import uk.gov.bis.controllers.auth.generateToken
import uk.gov.bis.db.oauth2.AuthCodeOps
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimAuthController @Inject()(GatewayAction: GatewayUserAction, authCodeDAO: AuthCodeOps)(implicit ec: ExecutionContext) extends Controller {

  /**
    * Handle the initial oAuth request
    */
  def authorize(scope: Option[String], clientId: String, redirectUri: String, state: Option[String]) = GatewayAction.async { implicit request =>
    scope match {
      case Some(s) => createAuthCode(s, clientId, redirectUri, state, request).map { url =>
        Redirect(url).removingFromSession(GatewayAction.sessionKey)
      }
      case None => Future.successful(BadRequest("missing scope"))
    }
  }

  def createAuthCode(scope: String, clientId: String, redirectUri: String, state: Option[String], request: GatewayIdRequest[AnyContent]): Future[String] = {
    val authCode = generateToken

    authCodeDAO.create(authCode, request.ggId.id, redirectUri, clientId, scope).map { _ =>
      state match {
        case Some(s) => s"$redirectUri?code=$authCode&state=${helper.urlEncode(s)}"
        case None => s"$redirectUri?code=$authCode"
      }
    }
  }

}
