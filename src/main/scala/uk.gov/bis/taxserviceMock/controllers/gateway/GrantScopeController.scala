package uk.gov.bis.taxserviceMock.controllers.gateway

import javax.inject.Inject

import play.api.mvc.Controller
import uk.gov.bis.taxserviceMock.actions.gateway.GatewayUserAction
import uk.gov.bis.taxserviceMock.data.{AuthCodeOps, AuthCodeRow, AuthIdOps}
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

class GrantScopeController @Inject()(UserAction: GatewayUserAction, auths: AuthIdOps, authCodes: AuthCodeOps)(implicit ec: ExecutionContext) extends Controller {

  def show(authId: Long) = UserAction.async { implicit request =>
    auths.get(authId).map {
      case Some(auth) => Ok(views.html.gateway.grantscope(auth, request.user))
      case None => BadRequest
    }
  }

  /**
    * If there is no AuthId record corresponding to the given code then it's a bad request.
    * Otherwise establish a new AuthCode record linked to the user and call back to the oAuth client
    *
    * @param authId
    * @return
    */
  def grantScope(authId: Long) = UserAction.async { implicit request =>
    import uk.gov.bis.taxserviceMock.auth.generateToken

    auths.pop(authId).flatMap {
      case None => Future.successful(BadRequest)
      case Some(auth) =>
        val token = generateToken
        val authCode = AuthCodeRow(token, request.user.gatewayID, "", System.currentTimeMillis(), Some(auth.scope), Some(auth.clientId), 4 * 60 * 60)
        authCodes.insert(authCode).map { _ =>
          val uri = auth.state match {
            case Some(s) => s"${auth.redirectUri}?code=$authCode&state=${helper.urlEncode(s)}"
            case None => s"${auth.redirectUri}?code=$authCode"
          }
          Redirect(auth.redirectUri).removingFromSession(UserAction.validatedUserKey)
        }
    }
  }
}
