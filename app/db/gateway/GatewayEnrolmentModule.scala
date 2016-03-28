package db.gateway

import javax.inject.{Inject, Singleton}

import db.{DBModule, SchemeModule}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

case class GatewayEnrolmentRow(gatewayUserId: Long, empref: String)

trait GatewayEnrolmentModule extends DBModule {
  self: GatewayUserModule with SchemeModule =>

  import driver.api._

  val GatewayEnrolments = TableQuery[GatewayEnrolmentTable]

  class GatewayEnrolmentTable(tag: Tag) extends Table[GatewayEnrolmentRow](tag, "gateway_enrolment") {
    def gatewayUserId = column[Long]("gateway_user_id")

    def gatewayUserFk = foreignKey("enrolment_user", gatewayUserId, GatewayUsers)(_.id, onDelete = ForeignKeyAction.Cascade)

    def empref = column[String]("empref")

    def schemeFk = foreignKey("enrolment_scheme", empref, Schemes)(_.empref, onDelete = ForeignKeyAction.Cascade)

    def * = (gatewayUserId, empref) <>(GatewayEnrolmentRow.tupled, GatewayEnrolmentRow.unapply)
  }

}

@Singleton
class GatewayEnrolmentDAO @Inject()()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends GatewayEnrolmentModule
    with GatewayUserModule
    with SchemeModule
