package uk.gov.bis.taxserviceMock.auth

import java.util.Date
import javax.inject.Inject

import cats.data.OptionT
import cats.instances.future._
import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.bis.taxserviceMock.data._

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

/**
  * Provides the behaviours needed by the OAuth2Provider to create and retrieve access tokens
  */
class APIDataHandler @Inject()(applications: ApplicationOps, accessTokens: AccessTokenOps, authCodes: AuthCodeOps, gatewayUsers: GatewayUserOps)(implicit ec: ExecutionContext) extends DataHandler[GatewayUser] {

  override def validateClient(request: AuthorizationRequest): Future[Boolean] = {
    Logger.debug("validate client")
    request.clientCredential match {
      case Some(cred) => applications.validate(cred.clientId, cred.clientSecret, request.grantType)
      case None => Future.successful(false)
    }
  }

  override def createAccessToken(authInfo: AuthInfo[GatewayUser]): Future[AccessToken] = {
    Logger.debug("create access token")
    val accessTokenExpiresIn = Some(60L * 60L) // 1 hour
    val refreshToken = Some(generateToken)
    val accessToken = generateToken
    val createdAt = System.currentTimeMillis()
    val tokenRow = AccessTokenRow(accessToken, refreshToken, authInfo.user.gatewayID, authInfo.scope, accessTokenExpiresIn, createdAt, authInfo.clientId.get)

    for {
      _ <- accessTokens.create(tokenRow)
    } yield AccessToken(accessToken, refreshToken, authInfo.scope, accessTokenExpiresIn, new Date(createdAt))
  }

  override def refreshAccessToken(authInfo: AuthInfo[GatewayUser], refreshToken: String): Future[AccessToken] = {
    Logger.debug("refresh access token")
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

  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[GatewayUser]]] = {
    Logger.debug("find auth info by refresh token")
    for {
      at <- OptionT(accessTokens.forRefreshToken(refreshToken))
      u <- OptionT(gatewayUsers.forGatewayID(at.gatewayID))
    } yield AuthInfo(u, Some(at.clientID), at.scope, None)
  }.value


  override def getStoredAccessToken(authInfo: AuthInfo[GatewayUser]): Future[Option[AccessToken]] = {
    Logger.debug("get stored access token using AuthInfo")
    OptionT(accessTokens.find(authInfo.user.gatewayID, authInfo.clientId)).map { token =>
      AccessToken(token.accessToken, token.refreshToken, token.scope, token.expiresIn, new Date(token.createdAt))
    }
  }.value

  override def findAccessToken(token: String): Future[Option[AccessToken]] = {
    Logger.debug("find access token by String")
    OptionT(accessTokens.forAccessToken(token)).map { token =>
      AccessToken(token.accessToken, token.refreshToken, token.scope, token.expiresIn, new Date(token.createdAt))
    }
  }.value

  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[GatewayUser]]] = {
    Logger.debug("find auth info by code")
    for {
      token <- OptionT(authCodes.find(code))
      user <- OptionT(gatewayUsers.forGatewayID(token.gatewayId))
    } yield AuthInfo(user, token.clientId, token.scope, None)
  }.value

  override def deleteAuthCode(code: String): Future[Unit] = {
    Logger.debug("delete auth code")
    authCodes.delete(code).map(_ => ())
  }

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[GatewayUser]]] = {
    Logger.debug("find auth info by access token")
    for {
      token <- OptionT(accessTokens.forAccessToken(accessToken.token))
      user <- OptionT(gatewayUsers.forGatewayID(token.gatewayID))
    } yield AuthInfo(user, Some(token.clientID), token.scope, None)
  }.value


  override def findUser(request: AuthorizationRequest): Future[Option[GatewayUser]] = {
    Logger.debug("find user by AuthorizationRequest")
    OptionT.fromOption(request.clientCredential).flatMap {
      cred =>
        OptionT(gatewayUsers.forGatewayID(cred.clientId)).filter {
          u =>
            BCrypt.checkpw(cred.clientSecret.get, u.password)
        }
    }
  }.value
}
