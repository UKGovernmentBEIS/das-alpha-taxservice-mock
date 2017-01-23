package uk.gov.bis.taxserviceMock.data

import scala.concurrent.{ExecutionContext, Future}

case class GatewayUser(gatewayID: String, password: String, empref: Option[String], name: String, require2SV: Option[Boolean])

trait GatewayUserOps {
  def forGatewayID(gatewayID: String)(implicit ec: ExecutionContext): Future[Option[GatewayUser]]

  def forEmpref(empref: String)(implicit ec: ExecutionContext): Future[Option[GatewayUser]]

  def validate(gatewayID: String, password: String)(implicit ec: ExecutionContext): Future[Option[GatewayUser]]
}
