package db.outh2

import javax.inject.Inject

import db.DBModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class AccessTokenRow(
                           accessToken: String,
                           refreshToken: Option[String],
                           gatewayId: String,
                           scope: Option[String],
                           expiresIn: Option[Long],
                           createdAt: Long,
                           clientId: String)

trait AccessTokenModule extends DBModule {

  import driver.api._

  implicit def ec: ExecutionContext

  val AccessTokens = TableQuery[AccessTokenTable]


  class AccessTokenTable(tag: Tag) extends Table[AccessTokenRow](tag, "access_token") {
    def accessToken = column[String]("access_token", O.PrimaryKey)

    def refreshToken = column[Option[String]]("refresh_token")

    def gatewayId = column[String]("gateway_id")

    def scope = column[Option[String]]("scope")

    def expiresIn = column[Option[Long]]("expires_in")

    def createdAt = column[Long]("created_at")

    def clientId = column[String]("client_id")

    def * = (accessToken, refreshToken, gatewayId, scope, expiresIn, createdAt, clientId) <>(AccessTokenRow.tupled, AccessTokenRow.unapply)

  }

}

class AccessTokenDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends AccessTokenModule {

  import driver.api._

  def forRefreshToken(refreshToken: String): Future[Option[AccessTokenRow]] = db.run {
    AccessTokens.filter(_.refreshToken === refreshToken).result.headOption
  }

  def find(gatewayId: String, clientId: Option[String]): Future[Option[AccessTokenRow]] = db.run {
    AccessTokens.filter(at => at.gatewayId === gatewayId && at.clientId == clientId).result.headOption
  }

  def deleteExistingAndCreate(token: AccessTokenRow): Future[Unit] = db.run {
    for {
      _ <- AccessTokens.filter(a => a.clientId === token.clientId && a.gatewayId === token.gatewayId).delete
      a <- AccessTokens += token
    } yield a.result
  }

}