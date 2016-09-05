package uk.gov.bis.taxserviceMock.data

import scala.concurrent.{ExecutionContext, Future}

case class AuthRecord(
                       accessToken: String,
                       refreshToken: Option[String],
                       gatewayID: String,
                       scope: Option[String],
                       expiresIn: Long,
                       createdAt: MongoDate,
                       clientID: String)

trait AuthRecordOps {
  def forRefreshToken(refreshToken: String)(implicit ec: ExecutionContext): Future[Option[AuthRecord]]

  def forAccessToken(accessToken: String)(implicit ec: ExecutionContext): Future[Option[AuthRecord]]

  def find(gatewayId: String, clientId: Option[String])(implicit ec: ExecutionContext): Future[Option[AuthRecord]]

  def create(token: AuthRecord)(implicit ec: ExecutionContext): Future[Unit]

  def deleteExistingAndCreate(token: AuthRecord)(implicit ec: ExecutionContext): Future[Unit]
}
