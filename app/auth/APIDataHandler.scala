package auth

import java.security.SecureRandom
import java.sql.Date
import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.std.future._
import db.gateway.{GatewayUserDAO, GatewayUserRow}
import db.outh2._
import org.apache.commons.codec.binary.Hex
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider._

case class Token(value: String, scope: String, expiresAt: Date)

object Token {
  implicit val formats = Json.format[Token]
}

@Singleton
class APIDataHandler @Inject()(ws: WSClient, clients: ClientDAO, accessTokens: AccessTokenDAO, authCodeDAO: AuthCodeDAO, gatewayUsers: GatewayUserDAO)(implicit ec: ExecutionContext) extends DataHandler[GatewayUserRow] {
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

  override def createAccessToken(authInfo: AuthInfo[GatewayUserRow]): Future[AccessToken] = {
    val accessTokenExpiresIn = Some(60L * 60L) // 1 hour
    val refreshToken = Some(generateToken)
    val accessToken = generateToken
    val createdAt = new Date(System.currentTimeMillis())
    val tokenRow = AccessTokenRow(accessToken, refreshToken, authInfo.user.id, authInfo.scope, accessTokenExpiresIn, createdAt, authInfo.clientId)

    for {
      _ <- accessTokens.deleteExistingAndCreate(tokenRow)
      _ <- sendTokenToApiServer(tokenRow)
    } yield AccessToken(accessToken, refreshToken, authInfo.scope, accessTokenExpiresIn, createdAt)
  }

  val apiServerEndpointUri = "http://localhost:9001/epaye/provide-token"

  def sendTokenToApiServer(t: AccessTokenRow): Future[Unit] = {
    val expiresIn = t.expiresIn.getOrElse(0L)
    val expiresAt = new DateTime(t.createdAt.getTime).plusSeconds(expiresIn.toInt)
    val token = Token(t.accessToken, t.scope.get, new Date(expiresAt.getMillis))

    val json = Json.toJson(token)

    ws.url(apiServerEndpointUri).put(json).map(_ => ())
  }

  override def refreshAccessToken(authInfo: AuthInfo[GatewayUserRow], refreshToken: String): Future[AccessToken] = {
    val accessTokenExpiresIn = Some(60L * 60L) // 1 hour
    val accessToken = generateToken
    val createdAt = new Date(System.currentTimeMillis())

    accessTokens.forRefreshToken(refreshToken).flatMap {
      case Some(accessTokenRow) =>
        val updatedRow = accessTokenRow.copy(accessToken = accessToken, createdAt = createdAt)
        for {
          _ <- accessTokens.deleteExistingAndCreate(updatedRow)
          _ <- sendTokenToApiServer(updatedRow)
        } yield AccessToken(updatedRow.accessToken, Some(refreshToken), authInfo.scope, accessTokenExpiresIn, createdAt)
    }
  }

  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[GatewayUserRow]]] = {
    val ot = for {
      at <- OptionT(accessTokens.forRefreshToken(refreshToken))
      u <- OptionT(gatewayUsers.byId(at.userId))
    } yield AuthInfo(u, at.clientId, at.scope, None)

    ot.value
  }


  override def getStoredAccessToken(authInfo: AuthInfo[GatewayUserRow]): Future[Option[AccessToken]] = {
    OptionT(accessTokens.find(authInfo.user.id, authInfo.clientId)).map {
      token =>
        AccessToken(token.accessToken, token.refreshToken, token.scope, token.expiresIn, token.createdAt)
    }.value
  }

  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[GatewayUserRow]]] = {
    val ot = for {
      token <- OptionT(authCodeDAO.find(code))
      user <- OptionT(gatewayUsers.byId(token.userId))
    } yield AuthInfo(user, token.clientId, token.scope, token.redirectUri)

    ot.value
  }

  override def deleteAuthCode(code: String): Future[Unit] = authCodeDAO.delete(code).map(_ => ())

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[GatewayUserRow]]] = ???

  override def findAccessToken(token: String): Future[Option[AccessToken]] = ???

  override def findUser(request: AuthorizationRequest): Future[Option[GatewayUserRow]] = {
    request.clientCredential.map {
      cred =>
        gatewayUsers.byName(cred.clientId).map(_.filter(u => BCrypt.checkpw(cred.clientSecret.get, u.hashedPassword)))
    } match {
      case None => Future.successful(None)
      case Some(f) => f
    }
  }
}
