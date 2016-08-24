package uk.gov.bis.taxserviceMock.data

import scala.concurrent.{ExecutionContext, Future}

case class AccessTokenRow(
                           accessToken: String,
                           refreshToken: Option[String],
                           gatewayID: String,
                           scope: Option[String],
                           expiresIn: Option[Long],
                           createdAt: Long,
                           clientID: String)

trait AccessTokenOps {
  def forRefreshToken(refreshToken: String)(implicit ec: ExecutionContext): Future[Option[AccessTokenRow]]

  def forAccessToken(accessToken: String)(implicit ec: ExecutionContext): Future[Option[AccessTokenRow]]

  def find(gatewayId: String, clientId: Option[String])(implicit ec: ExecutionContext): Future[Option[AccessTokenRow]]

  def create(token: AccessTokenRow)(implicit ec: ExecutionContext): Future[Unit]

  def deleteExistingAndCreate(token: AccessTokenRow)(implicit ec: ExecutionContext): Future[Unit]
}
