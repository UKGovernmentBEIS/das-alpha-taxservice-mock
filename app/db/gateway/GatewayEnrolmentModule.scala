package db.gateway

import javax.inject.Inject

import db.{SchemeModule, SlickModule}

import scala.concurrent.{ExecutionContext, Future}

case class GatewayEnrolmentRow(gatewayId: String, empref: String)

trait GatewayEnrolmentModule extends SlickModule {
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

class GatewayEnrolmentDAO @Inject()(enrolments: GatewayEnrolmentModule)(implicit val ec: ExecutionContext) {
  import enrolments._
  import api._

  def enrolledSchemes(gatewayId: String): Future[Seq[String]] = run {
    GatewayEnrolments.filter(_.gatewayId === gatewayId).map(_.empref).result
  }
}
