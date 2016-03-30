package db.gateway

import javax.inject.{Inject, Singleton}

import db.{DBModule, SchemeModule}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class GatewayEnrolmentRow(gatewayId: String, empref: String)

trait GatewayEnrolmentModule extends DBModule {
  self: GatewayIdModule with SchemeModule =>

  import driver.api._

  val GatewayEnrolments = TableQuery[GatewayEnrolmentTable]

  class GatewayEnrolmentTable(tag: Tag) extends Table[GatewayEnrolmentRow](tag, "gateway_enrolment") {
    def gatewayId = column[String]("gateway_id")

    def gatewayIdFk = foreignKey("enrolment_user", gatewayId, GatewayIds)(_.id, onDelete = ForeignKeyAction.Cascade)

    def empref = column[String]("empref")

    def schemeFk = foreignKey("enrolment_scheme", empref, Schemes)(_.empref, onDelete = ForeignKeyAction.Cascade)

    def * = (gatewayId, empref) <>(GatewayEnrolmentRow.tupled, GatewayEnrolmentRow.unapply)
  }

}

@Singleton
class GatewayEnrolmentDAO @Inject()()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends GatewayEnrolmentModule
    with GatewayIdModule
    with SchemeModule {
  import driver.api._

  def enrolledSchemes(gatewayId:String) : Future[Seq[String]] = db.run {
    GatewayEnrolments.filter(_.gatewayId === gatewayId).map(_.empref).result
  }
}
