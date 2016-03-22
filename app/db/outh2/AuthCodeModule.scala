package db.outh2

import java.sql.Date
import javax.inject.Inject

import db.DBModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class AuthCodeRow(authorizationCode: String, userId: Long, redirectUri: Option[String], createdAt: Date, scope: Option[String], clientId: Option[String], expiresIn: Int)

trait AuthCodeModule extends DBModule {

  import driver.api._

  def find(code: String): Future[Option[AuthCodeRow]] = db.run(AuthCodes.filter(_.authorizationCode === code).result.headOption)

  def delete(code: String): Future[Int] = db.run(AuthCodes.filter(_.authorizationCode === code).delete)

  def create(code: String, gatewayUserId: Long, clientId: String, empref:String): Future[Int] = {
    val r = AuthCodeRow(code, gatewayUserId, None, new Date(System.currentTimeMillis()), Some(empref), Some(clientId), 100000)
    db.run(AuthCodes += r)
  }

  val AuthCodes = TableQuery[AuthCodeTable]

  class AuthCodeTable(tag: Tag) extends Table[AuthCodeRow](tag, "AUTH_CODES") {
    def authorizationCode = column[String]("AUTHORIZATION_CODE", O.PrimaryKey)

    def userId = column[Long]("GATEWAY_USER_ID")

    def redirectUri = column[Option[String]]("REDIRECT_URI")

    def createdAt = column[Date]("CREATED_AT")

    def scope = column[Option[String]]("SCOPE")

    def clientId = column[Option[String]]("CLIENT_ID")

    def expiresIn = column[Int]("EXPIRES_IN")

    def * = (authorizationCode, userId, redirectUri, createdAt, scope, clientId, expiresIn) <>(AuthCodeRow.tupled, AuthCodeRow.unapply)
  }

}

class AuthCodeDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends AuthCodeModule
