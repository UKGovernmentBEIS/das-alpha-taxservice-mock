package auth

import java.security.SecureRandom
import java.util.Date
import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.std.future._
import config.ServiceConfig
import db.gateway.{GatewayEnrolmentDAO, GatewayIdDAO, GatewayIdRow}
import db.outh2._
import org.apache.commons.codec.binary.Hex
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider._

case class Token(value: String, scope: String, gatewayId: String, emprefs: List[String], clientId: String, expiresAt: Long)

object Token {
  implicit val formats = Json.format[Token]
}

@Singleton
class APIDataHandler @Inject()(config: ServiceConfig, ws: WSClient, clients: ClientDAO, accessTokens: AccessTokenDAO, authCodeDAO: AuthCodeDAO, gatewayIds: GatewayIdDAO, enrolments: GatewayEnrolmentDAO)(implicit ec: ExecutionContext) extends DataHandler[GatewayIdRow] {

  import config._

  override def validateClient(request: AuthorizationRequest): Future[Boolean] = {
    request.clientCredential match {
      case Some(cred) => clients.validate(cred.clientId, cred.clientSecret, request.grantType)
      case None => Future.successful(false)
    }
  }

  private val random = new SecureRandom()
  random.nextBytes(new Array[Byte](55))

  def generateToken: String = {
    val bytes = new Array[Byte](12)
    random.nextBytes(bytes)
    new String(Hex.encodeHex(bytes))
  }

  override def createAccessToken(authInfo: AuthInfo[GatewayIdRow]): Future[AccessToken] = {
    val accessTokenExpiresIn = Some(60L * 60L) // 1 hour
    val refreshToken = Some(generateToken)
    val accessToken = generateToken
    val createdAt = System.currentTimeMillis()
    val tokenRow = AccessTokenRow(accessToken, refreshToken, authInfo.user.id, authInfo.scope, accessTokenExpiresIn, createdAt, authInfo.clientId.get)

    for {
      _ <- accessTokens.deleteExistingAndCreate(tokenRow)
      _ <- sendTokenToApiServer(tokenRow)
    } yield AccessToken(accessToken, refreshToken, authInfo.scope, accessTokenExpiresIn, new Date(createdAt))
  }

  def sendTokenToApiServer(t: AccessTokenRow): Future[Unit] = {
    val expiresIn = t.expiresIn.getOrElse(0L)
    val expiresAt = new DateTime(t.createdAt).plusSeconds(expiresIn.toInt)

    enrolments.enrolledSchemes(t.gatewayId).flatMap { emprefs =>
      val token = Token(t.accessToken, t.scope.get, t.gatewayId, emprefs.toList, t.clientId, expiresAt.getMillis)

      val json = Json.toJson(token)
      Logger.info(Json.prettyPrint(json))

      ws.url(s"$apiServerEndpointUri/provide-token").put(json).map(_ => ())
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

  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[GatewayIdRow]]] = {
    for {
      token <- OptionT(authCodeDAO.find(code))
      user <- OptionT(gatewayIds.byId(token.gatewayId))
    } yield AuthInfo(user, token.clientId, token.scope, None)
  }.value

  override def deleteAuthCode(code: String): Future[Unit] = authCodeDAO.delete(code).map(_ => ())

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[GatewayIdRow]]] = ???

  override def findAccessToken(token: String): Future[Option[AccessToken]] = ???

  override def findUser(request: AuthorizationRequest): Future[Option[GatewayIdRow]] = {
    OptionT.fromOption(request.clientCredential).flatMap { cred =>
      OptionT(gatewayIds.byId(cred.clientId)).filter { u =>
        BCrypt.checkpw(cred.clientSecret.get, u.hashedPassword)
      }
    }
  }.value
}
