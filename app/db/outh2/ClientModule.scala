package db.outh2

import javax.inject.Inject

import db.DBModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class ClientRow(id: String, secret: Option[String], redirectUri: Option[String], scope: Option[String])

trait ClientModule extends DBModule {

  import driver.api._

  val Clients = TableQuery[ClientTable]

  def validate(id: String, secret: Option[String], grantType: String): Future[Boolean] = db.run {
    Clients.filter(c => c.id === id).result.headOption.map(_.isDefined)
  }

  class ClientTable(tag: Tag) extends Table[ClientRow](tag, "client") {
    def id = column[String]("id", O.PrimaryKey)

    def secret = column[Option[String]]("secret")

    def redirectUri = column[Option[String]]("redirect_uri")

    def scope = column[Option[String]]("scope")

    def grantType = column[String]("grant_type")

    def * = (id, secret, redirectUri, scope) <>(ClientRow.tupled, ClientRow.unapply)
  }

}

class ClientDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends ClientModule