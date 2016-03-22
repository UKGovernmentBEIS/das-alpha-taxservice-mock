package db.gateway

import javax.inject.{Inject, Singleton}

import db.DBModule
import db.levy.SchemeModule
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

case class GatewayEnrolmentRow(gatewayUserId: Long, empref: String)

trait GatewayEnrolmentModule extends DBModule {
  self: GatewayUserModule with SchemeModule =>

  import driver.api._

  val GatewayEnrolments = TableQuery[GatewayEnrolmentTable]

  class GatewayEnrolmentTable(tag: Tag) extends Table[GatewayEnrolmentRow](tag, "GATEWAY_ENROLMENT") {
    def gatewayUserId = column[Long]("GATEWAY_USER_ID")

    def gatewayUserFk = foreignKey("enrolment_user", gatewayUserId, GatewayUsers)(_.id, onDelete = ForeignKeyAction.Cascade)

    def empref = column[String]("EMPREF")

    def schemeFk = foreignKey("enrolment_scheme", empref, Schemes)(_.empref, onDelete = ForeignKeyAction.Cascade)

    def * = (gatewayUserId, empref) <>(GatewayEnrolmentRow.tupled, GatewayEnrolmentRow.unapply)
  }

}

@Singleton
class GatewayEnrolmentDAO @Inject()()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext)
  extends GatewayEnrolmentModule
    with GatewayUserModule
    with SchemeModule
