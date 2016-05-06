package uk.gov.bis.taxserviceMock.db.oauth2

import javax.inject.Inject

import com.google.inject.ImplementedBy
import uk.gov.bis.taxserviceMock.db.SlickModule
import uk.gov.bis.taxserviceMock.db.gateway.GatewayIdModule
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

trait AccessTokenModule extends SlickModule {
  self: GatewayIdModule =>

  import driver.api._

  implicit def ec: ExecutionContext

  val AccessTokens = TableQuery[AccessTokenTable]


  class AccessTokenTable(tag: Tag) extends Table[AccessTokenRow](tag, "access_token") {
    def accessToken = column[String]("access_token", O.PrimaryKey)

    def refreshToken = column[Option[String]]("refresh_token")

    def gatewayId = column[String]("gateway_id")

    def gatewayIdFk = foreignKey("token_gateway_id_fk", gatewayId, GatewayIds)(_.id, onDelete = ForeignKeyAction.Cascade)

    def scope = column[Option[String]]("scope")

    def expiresIn = column[Option[Long]]("expires_in")

    def createdAt = column[Long]("created_at")

    def clientId = column[String]("client_id")

    def * = (accessToken, refreshToken, gatewayId, scope, expiresIn, createdAt, clientId) <>(AccessTokenRow.tupled, AccessTokenRow.unapply)

  }

}

@ImplementedBy(classOf[AccessTokenDAO])
trait AccessTokenOps {
  def forRefreshToken(refreshToken: String): Future[Option[AccessTokenRow]]

  def forAccessToken(accessToken: String): Future[Option[AccessTokenRow]]

  def find(gatewayId: String, clientId: Option[String]): Future[Option[AccessTokenRow]]

  def create(token: AccessTokenRow): Future[Unit]

  def deleteExistingAndCreate(token: AccessTokenRow): Future[Unit]
}

class AccessTokenDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends AccessTokenModule with AccessTokenOps with GatewayIdModule {

  import driver.api._

  def forRefreshToken(refreshToken: String): Future[Option[AccessTokenRow]] = db.run {
    AccessTokens.filter(_.refreshToken === refreshToken).result.headOption
  }

  override def forAccessToken(accessToken: String): Future[Option[AccessTokenRow]] = db.run {
    AccessTokens.filter(_.accessToken === accessToken).result.headOption
  }

  def find(gatewayId: String, clientId: Option[String]): Future[Option[AccessTokenRow]] = db.run {
    AccessTokens.filter(at => at.gatewayId === gatewayId && at.clientId == clientId).result.headOption
  }

  def create(token: AccessTokenRow): Future[Unit] = db.run(AccessTokens += token).map(_ => ())

  def deleteExistingAndCreate(token: AccessTokenRow): Future[Unit] = db.run {
    for {
      _ <- AccessTokens.filter(a => a.refreshToken === token.refreshToken && a.clientId === token.clientId && a.gatewayId === token.gatewayId).delete
      a <- AccessTokens += token
    } yield a.result
  }


}