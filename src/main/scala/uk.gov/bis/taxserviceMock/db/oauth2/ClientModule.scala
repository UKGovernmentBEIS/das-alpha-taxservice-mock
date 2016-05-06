package uk.gov.bis.taxserviceMock.db.oauth2

import javax.inject.Inject

import uk.gov.bis.taxserviceMock.db.SlickModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class ClientRow(id: String, secret: Option[String], redirectUri: Option[String], scope: Option[String], grantType: String = "authorization_code")

trait ClientModule extends SlickModule {

  import driver.api._

  val Clients = TableQuery[ClientTable]

  class ClientTable(tag: Tag) extends Table[ClientRow](tag, "client") {
    def id = column[String]("id", O.PrimaryKey)

    def secret = column[Option[String]]("secret")

    def redirectUri = column[Option[String]]("redirect_uri")

    def scope = column[Option[String]]("scope")

    def grantType = column[String]("grant_type")

    def * = (id, secret, redirectUri, scope, grantType) <>(ClientRow.tupled, ClientRow.unapply)
  }

}

class ClientDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends ClientModule {

  import driver.api._

  def all(): Future[Seq[ClientRow]] = db.run(Clients.result)

  def remove(id: String): Future[Int] = db.run(Clients.filter(_.id === id).delete)

  def addClient(row: ClientRow): Future[Int] = db.run(Clients += row)

  def validate(id: String, secret: Option[String], grantType: String): Future[Boolean] = db.run {
    Clients.filter(c => c.id === id).result.headOption.map(_.isDefined)
  }
}