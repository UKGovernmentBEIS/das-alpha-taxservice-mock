package uk.gov.bis.taxserviceMock.controllers

import javax.inject.Inject

import cats.data.EitherT
import cats.instances.future._
import cats.syntax.either._
import play.api.Logger
import play.api.mvc.Controller
import uk.gov.bis.taxserviceMock.actions.GatewayUserAction
import uk.gov.bis.taxserviceMock.data._
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

class GrantScopeController @Inject()(UserAction: GatewayUserAction, auths: AuthRequestOps, authCodes: AuthCodeOps, scopeOps: ScopeOps)(implicit ec: ExecutionContext) extends Controller {

  implicit class ErrorSyntax[A](ao: Option[A]) {
    def orError(err: String): Either[String, A] = ao.fold[Either[String, A]](Left(err))(a => Right(a))
  }

  def show(authId: Long) = UserAction.async { implicit request =>
    val x = for {
      a <- EitherT(auths.get(authId).map(_.orError("unknown auth id")))
      scopes = a.scope.split(" ").filter(_ != "")
      s <- EitherT(scopeOps.byNames(scopes).map(_.leftMap(badScopes => s"Unknown scopes: ${badScopes.mkString(" , ")}")))
    } yield (a, s)

    x.value.flatMap {
      case Right((auth, scopes)) if scopes.exists(_.needsExplicitGrant.contains(true)) => Future.successful(Ok(views.html.grantscope(auth.id, request.user.name, scopes.map(_.description))))
      case Right((auth, scopes))                                                       => grantScope(auth.id)(request)
      case Left(err)                                                                   => Future.successful(BadRequest(err))
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
          Logger.debug(s"Callback URL is $uri")
          Redirect(uri).removingFromSession(UserAction.validatedUserKey)
        }
    }
  }
}
