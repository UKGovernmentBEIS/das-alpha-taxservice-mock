package db.outh2

import java.sql.Date
import javax.inject.Inject

import db.DBModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class AccessTokenRow(
                           accessToken: String,
                           refreshToken: Option[String],
                           userId: Long, scope: Option[String],
                           expiresIn: Option[Long],
                           createdAt: Date,
                           clientId: Option[String])

trait AccessTokenModule extends DBModule {

  import driver.api._

  implicit def ec: ExecutionContext

  val AccessTokens = TableQuery[AccessTokenTable]

  def all(): Future[Seq[AccessTokenRow]] = db.run(AccessTokens.result)

  def find(userId:Long, clientId:Option[String]):Future[Option[AccessTokenRow]] = db.run {
    AccessTokens.filter(at => at.userId === userId && at.clientId == clientId).result.headOption
  }

  def deleteExistingAndCreate(token: AccessTokenRow): Future[Unit] = db.run {
    for {
      _ <- AccessTokens.filter(a => a.clientId === token.clientId && a.userId === token.userId).delete
      a <- AccessTokens += token
    } yield a.result
  }

  class AccessTokenTable(tag: Tag) extends Table[AccessTokenRow](tag, "access_token") {
    def accessToken = column[String]("access_token", O.PrimaryKey)

    def refreshToken = column[Option[String]]("refresh_token")

    def userId = column[Long]("gateway_user_id")

    def scope = column[Option[String]]("scope")

    def expiresIn = column[Option[Long]]("expires_in")

    def createdAt = column[Date]("created_at")

    def clientId = column[Option[String]]("client_id")

    def * = (accessToken, refreshToken, userId, scope, expiresIn, createdAt, clientId) <>(AccessTokenRow.tupled, AccessTokenRow.unapply)

  }

}

class AccessTokenDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends AccessTokenModule