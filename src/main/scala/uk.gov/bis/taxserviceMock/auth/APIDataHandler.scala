package uk.gov.bis.taxserviceMock.auth

import java.util.Date
import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.std.future._
import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.bis.taxserviceMock.data.{AccessTokenOps, AccessTokenRow}
import uk.gov.bis.taxserviceMock.db.gateway.{GatewayIdDAO, GatewayIdRow}
import uk.gov.bis.taxserviceMock.db.oauth2._

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider._

case class ServiceBinding(service: String, identifierType: String, identifier: String)

object ServiceBinding {
  implicit val formats = Json.format[ServiceBinding]
}

case class Token(value: String, scopes: List[String], gatewayId: String, enrolments: List[ServiceBinding], clientId: String, expiresAt: Long)

object Token {
  implicit val formats = Json.format[Token]
}

@Singleton
class APIDataHandler @Inject()( ws: WSClient, clients: ClientDAO, accessTokens: AccessTokenOps, authCodes: AuthCodeOps, gatewayIds: GatewayIdDAO)(implicit ec: ExecutionContext) extends DataHandler[GatewayIdRow] {

  override def validateClient(request: AuthorizationRequest): Future[Boolean] = {
    request.clientCredential match {
      case Some(cred) => clients.validate(cred.clientId, cred.clientSecret, request.grantType)
      case None => Future.successful(false)
    }
  }

  override def createAccessToken(authInfo: AuthInfo[GatewayIdRow]): Future[AccessToken] = {
    val accessTokenExpiresIn = Some(60L * 60L) // 1 hour
    val refreshToken = Some(generateToken)
    val accessToken = generateToken
    val createdAt = System.currentTimeMillis()
    val tokenRow = AccessTokenRow(accessToken, refreshToken, authInfo.user.id, authInfo.scope, accessTokenExpiresIn, createdAt, authInfo.clientId.get)

    for {
      _ <- accessTokens.create(tokenRow)
    } yield AccessToken(accessToken, refreshToken, authInfo.scope, accessTokenExpiresIn, new Date(createdAt))
  }

  override def refreshAccessToken(authInfo: AuthInfo[GatewayIdRow], refreshToken: String): Future[AccessToken] = {
    val accessTokenExpiresIn = Some(60L * 60L) // 1 hour
    val accessToken = generateToken
    val createdAt = System.currentTimeMillis()

    accessTokens.forRefreshToken(refreshToken).flatMap {
      case Some(accessTokenRow) =>
        val updatedRow = accessTokenRow.copy(accessToken = accessToken, createdAt = createdAt)
        for {
          _ <- accessTokens.deleteExistingAndCreate(updatedRow)
        } yield AccessToken(updatedRow.accessToken, Some(refreshToken), authInfo.scope, accessTokenExpiresIn, new Date(createdAt))
      case None =>
        val s = s"Cannot find an access token entry with refresh token $refreshToken"
        Logger.warn(s)
        throw new IllegalArgumentException(s)
    }
  }

  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[GatewayIdRow]]] = {
    for {
      at <- OptionT(accessTokens.forRefreshToken(refreshToken))
      u <- OptionT(gatewayIds.byId(at.gatewayId))
    } yield AuthInfo(u, Some(at.clientId), at.scope, None)
  }.value


  override def getStoredAccessToken(authInfo: AuthInfo[GatewayIdRow]): Future[Option[AccessToken]] = {
    OptionT(accessTokens.find(authInfo.user.id, authInfo.clientId)).map { token =>
      AccessToken(token.accessToken, token.refreshToken, token.scope, token.expiresIn, new Date(token.createdAt))
    }
  }.value

  override def findAccessToken(token: String): Future[Option[AccessToken]] = {
    OptionT(accessTokens.forAccessToken(token)).map { token =>
      AccessToken(token.accessToken, token.refreshToken, token.scope, token.expiresIn, new Date(token.createdAt))
    }
  }.value

  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[GatewayIdRow]]] = {
    for {
      token <- OptionT(authCodes.find(code))
      user <- OptionT(gatewayIds.byId(token.gatewayId))
    } yield AuthInfo(user, token.clientId, token.scope, None)
  }.value

  override def deleteAuthCode(code: String): Future[Unit] = authCodes.delete(code).map(_ => ())

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[GatewayIdRow]]] = {
    for {
      token <- OptionT(accessTokens.forAccessToken(accessToken.token))
      user <- OptionT(gatewayIds.byId(token.gatewayId))
    } yield AuthInfo(user, Some(token.clientId), token.scope, None)
  }.value


  override def findUser(request: AuthorizationRequest): Future[Option[GatewayIdRow]] = {
    OptionT.fromOption(request.clientCredential).flatMap {
      cred =>
        OptionT(gatewayIds.byId(cred.clientId)).filter {
          u =>
            BCrypt.checkpw(cred.clientSecret.get, u.hashedPassword)
        }
    }
  }.value
}
