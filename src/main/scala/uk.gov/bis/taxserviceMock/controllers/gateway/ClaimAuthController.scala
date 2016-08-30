package uk.gov.bis.taxserviceMock.controllers.gateway

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.mvc.{AnyContent, Controller}
import uk.gov.bis.taxserviceMock.actions.gateway.{GatewayUserAction, GatewayUserRequest}
import uk.gov.bis.taxserviceMock.auth.generateToken
import uk.gov.bis.taxserviceMock.data.AuthCodeOps
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimAuthController @Inject()(GatewayAction: GatewayUserAction, authCodes: AuthCodeOps)(implicit ec: ExecutionContext) extends Controller {

  /**
    * Handle the initial oAuth request
    */
  def authorize(scope: Option[String], clientId: String, redirectUri: String, state: Option[String]) = GatewayAction.async { implicit request =>
    Logger.debug("authorize")
    scope match {
      case Some(s) =>
        createAuthCode(s, clientId, redirectUri, state, request).map { url =>
          Redirect(url).removingFromSession(GatewayAction.sessionKey)
        }
      case None => Future.successful(BadRequest("missing scope"))
    }
  }


  def createAuthCode(scope: String, clientId: String, redirectUri: String, state: Option[String], request: GatewayUserRequest[AnyContent]): Future[String] = {
    val authCode = generateToken

    authCodes.create(authCode, request.user.gatewayID, redirectUri, clientId, scope).map { _ =>
      state match {
        case Some(s) => s"$redirectUri?code=$authCode&state=${helper.urlEncode(s)}"
        case None => s"$redirectUri?code=$authCode"
      }
    }
  }

}
