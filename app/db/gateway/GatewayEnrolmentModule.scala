package db.gateway

import javax.inject.Inject

import db.{SchemeModule, SlickModule}

import scala.concurrent.{ExecutionContext, Future}

case class GatewayEnrolmentRow(gatewayId: String, service: String, taxIdType: String, taxId: String)

trait GatewayEnrolmentModule extends SlickModule {
  self: GatewayIdModule with SchemeModule =>

  import driver.api._

  val GatewayEnrolments = TableQuery[GatewayEnrolmentTable]

  class GatewayEnrolmentTable(tag: Tag) extends Table[GatewayEnrolmentRow](tag, "gateway_enrolment") {
    def gatewayId = column[String]("gateway_id")

    def gatewayIdFk = foreignKey("enrolment_user", gatewayId, GatewayIds)(_.id, onDelete = ForeignKeyAction.Cascade)

    def service = column[String]("service")

    def taxIdType = column[String]("tax_id_type")

    def taxId = column[String]("tax_id")

    def * = (gatewayId, service, taxIdType, taxId) <>(GatewayEnrolmentRow.tupled, GatewayEnrolmentRow.unapply)
  }

}

class GatewayEnrolmentDAO @Inject()(enrolments: GatewayEnrolmentModule)(implicit val ec: ExecutionContext) {

  import enrolments._
  import api._

  def find(gatewayId: String): Future[Seq[GatewayEnrolmentRow]] = run(GatewayEnrolments.filter(_.gatewayId === gatewayId).result)

  def enrolledSchemes(gatewayId: String): Future[Seq[String]] = run {
    GatewayEnrolments.filter(ge => ge.gatewayId === gatewayId && ge.service === "epaye" && ge.taxIdType === "empref").map(_.taxId).result
  }
}
