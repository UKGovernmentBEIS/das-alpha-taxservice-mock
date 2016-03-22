package db.gateway

import javax.inject.{Inject, Singleton}

import db.DBModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class GatewayUserRow(id: Long, name: String, hashedPassword: String)

trait GatewayUserModule extends DBModule {

  import driver.api._

  val GatewayUsers = TableQuery[GatewayUserTable]

  def all(): Future[Seq[GatewayUserRow]] = db.run(GatewayUsers.result)

  def validate(username:String, password:String):Future[Option[GatewayUserRow]] = db.run{
    GatewayUsers.filter(u => u.name === username && u.password === password).result.headOption
  }

  def byId(id: Long): Future[Option[GatewayUserRow]] = db.run(GatewayUsers.filter(_.id === id).result.headOption)

  def byName(s: String): Future[Option[GatewayUserRow]] = db.run(GatewayUsers.filter(u => u.name === s).result.headOption)

  class GatewayUserTable(tag: Tag) extends Table[GatewayUserRow](tag, "GATEWAY_USER") {

    def id = column[Long]("ID", O.PrimaryKey)

    def name = column[String]("USERNAME")

    def password = column[String]("PASSWORD")

    def * = (id, name, password) <>(GatewayUserRow.tupled, GatewayUserRow.unapply)

  }

}

@Singleton
class GatewayUserDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends GatewayUserModule