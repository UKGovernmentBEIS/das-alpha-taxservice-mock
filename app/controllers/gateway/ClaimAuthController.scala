package controllers.gateway

import javax.inject.{Inject, Singleton}

import actions.gateway.GatewayUserAction
import db.oauth2.AuthCodeOps
import play.api.mvc.{Action, Controller}
import views.html.helper

import scala.concurrent.ExecutionContext

@Singleton
class ClaimAuthController @Inject()(GatewayAction: GatewayUserAction, authCodeDAO: AuthCodeOps)(implicit ec: ExecutionContext) extends Controller {

  /**
    * Handle the initial oAuth request
    */
  def authorize(scope: Option[String], clientId: String, redirectUri: String, state: Option[String]) = GatewayAction { implicit request =>
    scope match {
      case Some(s) => Ok(views.html.gateway.claim(s, clientId, redirectUri, state))
      case None => BadRequest("missing scope")
    }
  }

  def confirm(scope: String, clientId: String, redirectUri: String, state: Option[String]) = GatewayAction.async { implicit request =>
    val authCode = auth.generateToken

    authCodeDAO.create(authCode, request.ggId.id, redirectUri, clientId, scope).map { _ =>
      val url = state match {
        case Some(s) => s"$redirectUri?code=$authCode&state=${helper.urlEncode(s)}"
        case None => s"$redirectUri?code=$authCode"
      }

      Redirect(url).removingFromSession(GatewayAction.sessionKey)
    }
  }

  def deny(redirectUri: String) = Action { implicit r =>
    Redirect(redirectUri).removingFromSession(GatewayAction.sessionKey)
  }

}
