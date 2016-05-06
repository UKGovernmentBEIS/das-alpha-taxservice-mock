package uk.gov.bis.taxserviceMock.auth

import java.util.Date
import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.std.future._
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.bis.taxserviceMock.config.ServiceConfig
import uk.gov.bis.taxserviceMock.db.gateway.{GatewayEnrolmentDAO, GatewayIdDAO, GatewayIdRow}
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
class APIDataHandler @Inject()(config: ServiceConfig, ws: WSClient, clients: ClientDAO, accessTokens: AccessTokenOps, authCodes: AuthCodeOps, gatewayIds: GatewayIdDAO, enrolments: GatewayEnrolmentDAO)(implicit ec: ExecutionContext) extends DataHandler[GatewayIdRow] {

  import config._

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
      _ <- sendTokenToApiServer(tokenRow)
    } yield AccessToken(accessToken, refreshToken, authInfo.scope, accessTokenExpiresIn, new Date(createdAt))
  }

  def sendTokenToApiServer(t: AccessTokenRow): Future[Unit] = {
    val expiresIn = t.expiresIn.getOrElse(0L)
    val expiresAt = new DateTime(t.createdAt).plusSeconds(expiresIn.toInt)

    enrolments.find(t.gatewayId).flatMap { emprefs =>
      val serviceBindings = emprefs.map(e => ServiceBinding(e.service, e.taxIdType, e.taxId)).toList
      val token = Token(t.accessToken, t.scope.get.split("\\s").toList, t.gatewayId, serviceBindings, t.clientId, expiresAt.getMillis)

      ws.url(s"$apiHost/auth/provide-token").put(Json.toJson(token)).map { response =>
        response.status match {
          case s if s >= 200 && s <= 299 => ()
          case s => throw new Exception(s"/auth/provide-token call resulted in status $s with body ${response.body}")
        }
      }
    }
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
          _ <- sendTokenToApiServer(updatedRow)
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
