package uk.gov.bis.taxserviceMock.controllers

import javax.inject.Inject

import cats.data.Xor.{Left, Right}
import cats.data.{Xor, XorT}
import cats.instances.future._
import cats.syntax.xor._
import play.api.mvc.Controller
import uk.gov.bis.taxserviceMock.actions.GatewayUserAction
import uk.gov.bis.taxserviceMock.data.{AuthCodeOps, AuthCodeRow, AuthRequestOps, ScopeOps}
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

class GrantScopeController @Inject()(UserAction: GatewayUserAction, auths: AuthRequestOps, authCodes: AuthCodeOps, scopes: ScopeOps)(implicit ec: ExecutionContext) extends Controller {

  implicit class ErrorSyntax[A](ao: Option[A]) {
    def orError(err: String): Xor[String, A] = ao.fold[Xor[String, A]](err.left)(a => a.right)
  }

  def show(authId: Long) = UserAction.async { implicit request =>
    val x = for {
      a <- XorT(auths.get(authId).map(_.orError("unknown auth id")))
      s <- XorT(scopes.byName(a.scope).map(_.orError("unknown scope")))
    } yield (a, s)

    x.value.flatMap {
      case Right((auth, scope)) if scope.needsExplicitGrant.contains(true) => Future.successful(Ok(views.html.grantscope(auth.id, request.user.name, scope.description)))
      case Right((auth, scope))                                            => grantScope(auth.id)(request)
      case Left(err)                                                       => Future.successful(BadRequest(err))
    }
  }

  def cancel(authId: Long) = UserAction.async { implicit request =>
    auths.pop(authId).map(_.orError("unknown auth id")).map {
      case Right(auth) => Redirect(s"${auth.redirectUri}?error=access_denied&error_description=user+denied+the+authorization&error_code=USER_DENIED_AUTHORIZATION")
      case Left(err)   => BadRequest(err)
    }
  }

  /**
    * If there is no AuthId record corresponding to the given code then it's a bad request.
    * Otherwise establish a new AuthCode record linked to the user and call back to the oAuth client
    */
  def grantScope(authId: Long) = UserAction.async { implicit request =>
    import uk.gov.bis.taxserviceMock.auth.generateToken

    auths.pop(authId).flatMap {
      case None       => Future.successful(BadRequest)
      case Some(auth) =>
        val token = generateToken
        val authCode = AuthCodeRow(token, request.user.gatewayID, "", System.currentTimeMillis(), Some(auth.scope), Some(auth.clientId), 4 * 60 * 60)
        authCodes.insert(authCode).map { _ =>
          val uri = auth.state match {
            case Some(s) => s"${auth.redirectUri}?code=${authCode.authorizationCode}&state=${helper.urlEncode(s)}"
            case None    => s"${auth.redirectUri}?code=${authCode.authorizationCode}"
          }
          Redirect(uri).removingFromSession(UserAction.validatedUserKey)
        }
    }
  }
}
