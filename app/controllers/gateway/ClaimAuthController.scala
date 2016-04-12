package controllers.gateway

import java.security.SecureRandom
import javax.inject.{Inject, Singleton}

import actions.gateway.GatewayUserAction
import db.oauth2.AuthCodeOps
import org.apache.commons.codec.binary.Hex
import play.api.mvc.{Action, Controller}
import views.html.helper

import scala.concurrent.ExecutionContext

@Singleton
class ClaimAuthController @Inject()(GatewayAction: GatewayUserAction, authCodeDAO: AuthCodeOps)(implicit ec: ExecutionContext) extends Controller {

  /**
    * Handle the initial oAuth request
    */
  def auth(scope: Option[String], clientId: String, redirectUri: String, state: Option[String]) = GatewayAction { implicit request =>
    scope match {
      case Some(s) => Ok(views.html.gateway.claim(s, clientId, redirectUri, state))
      case None => BadRequest("missing scope")
    }
  }

  private val random = new SecureRandom()
  random.nextBytes(new Array[Byte](55))

  def generateToken: String = {
    val bytes = new Array[Byte](12)
    random.nextBytes(bytes)
    new String(Hex.encodeHex(bytes))
  }

  def confirm(scope: String, clientId: String, redirectUri: String, state: Option[String]) = GatewayAction.async { implicit request =>
    val authCode = generateToken

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
