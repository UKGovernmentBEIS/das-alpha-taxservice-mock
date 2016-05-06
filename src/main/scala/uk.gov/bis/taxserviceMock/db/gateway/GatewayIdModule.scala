package uk.gov.bis.taxserviceMock.db.gateway

import javax.inject.{Inject, Singleton}

import uk.gov.bis.taxserviceMock.db.SlickModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class GatewayIdRow(id: String, hashedPassword: String)

trait GatewayIdModule extends SlickModule {

  import driver.api._

  val GatewayIds = TableQuery[GatewayUserTable]

  def all(): Future[Seq[GatewayIdRow]] = db.run(GatewayIds.result)

  def validate(gatewayId:String, password:String):Future[Option[GatewayIdRow]] = db.run{
    GatewayIds.filter(u => u.id === gatewayId && u.password === password).result.headOption
  }

  def byId(id: String): Future[Option[GatewayIdRow]] = db.run(GatewayIds.filter(_.id === id).result.headOption)

  class GatewayUserTable(tag: Tag) extends Table[GatewayIdRow](tag, "gateway_id") {

    def id = column[String]("id")

    def password = column[String]("password")

    def * = (id, password) <>(GatewayIdRow.tupled, GatewayIdRow.unapply)

  }

}

@Singleton
class GatewayIdDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends GatewayIdModule