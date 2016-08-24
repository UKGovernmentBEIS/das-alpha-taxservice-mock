package uk.gov.bis.taxserviceMock.data

import scala.concurrent.{ExecutionContext, Future}

case class Application(applicationID: String, clientID: String, clientSecret: String, serverToken: String)

trait ApplicationOps {
  def validate(id: String, secret: Option[String], grantType: String)(implicit ec: ExecutionContext): Future[Boolean]
}
