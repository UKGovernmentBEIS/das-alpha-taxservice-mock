package controllers.gateway

import java.security.SecureRandom

import actions.gateway.GatewayUserAction
import javax.inject.{Singleton, Inject}
import db.outh2.AuthCodeDAO
import org.apache.commons.codec.binary.Hex
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

@Singleton
class ClaimAuthController @Inject()(UserAction: GatewayUserAction, authCodeDAO: AuthCodeDAO)(implicit ec:ExecutionContext) extends Controller {

  def auth(empref: String, clientId: String, redirectUri: String) = UserAction { implicit request =>
    Ok(views.html.gateway.claim(empref, clientId, redirectUri))
  }

  private val random = new SecureRandom()
  random.nextBytes(new Array[Byte](55))

  def generateToken: String = {
    val bytes = new Array[Byte](12)
    random.nextBytes(bytes)
    new String(Hex.encodeHex(bytes))
  }

  def confirm(empref: String, clientId: String, redirectUri: String) = UserAction.async { implicit request =>
    val authCode = generateToken
    authCodeDAO.create(authCode, request.user.id, clientId, empref).map { _ =>
      Redirect(s"$redirectUri?code=$authCode").removingFromSession(UserAction.sessionKey)
    }
  }

  def deny(redirectUri: String) = Action { implicit r =>
    Redirect(redirectUri).removingFromSession(UserAction.sessionKey)
  }

}
