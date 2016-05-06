package uk.gov.bis.taxserviceMock.db.oauth2

import javax.inject.Inject

import com.google.inject.ImplementedBy
import uk.gov.bis.taxserviceMock.db.SlickModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class AuthCodeRow(authorizationCode: String, gatewayId: String, redirectUri: String, createdAt: Long, scope: Option[String], clientId: Option[String], expiresIn: Int)

trait AuthCodeModule extends SlickModule {

  import driver.api._

  val AuthCodes = TableQuery[AuthCodeTable]

  class AuthCodeTable(tag: Tag) extends Table[AuthCodeRow](tag, "auth_codes") {
    def authorizationCode = column[String]("authorization_code", O.PrimaryKey)

    def gatewayId = column[String]("gateway_id")

    def redirectUri = column[String]("redirect_uri")

    def createdAt = column[Long]("created_at")

    def scope = column[Option[String]]("scope")

    def clientId = column[Option[String]]("client_id")

    def expiresIn = column[Int]("expires_in")

    def * = (authorizationCode, gatewayId, redirectUri, createdAt, scope, clientId, expiresIn) <>(AuthCodeRow.tupled, AuthCodeRow.unapply)
  }

}

@ImplementedBy(classOf[AuthCodeDAO])
trait AuthCodeOps {
  def find(code: String): Future[Option[AuthCodeRow]]

  def delete(code: String): Future[Int]

  def create(code: String, gatewayUserId: String, redirectUri: String, clientId: String, scope: String): Future[Int]
}

class AuthCodeDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends AuthCodeModule with AuthCodeOps {

  import driver.api._

  def find(code: String): Future[Option[AuthCodeRow]] = db.run(AuthCodes.filter(_.authorizationCode === code).result.headOption)

  def delete(code: String): Future[Int] = db.run(AuthCodes.filter(_.authorizationCode === code).delete)

  def create(code: String, gatewayUserId: String, redirectUri: String, clientId: String, scope: String): Future[Int] = {
    val r = AuthCodeRow(code, gatewayUserId, redirectUri, System.currentTimeMillis(), Some(scope), Some(clientId), 100000)
    db.run(AuthCodes += r)
  }
}
