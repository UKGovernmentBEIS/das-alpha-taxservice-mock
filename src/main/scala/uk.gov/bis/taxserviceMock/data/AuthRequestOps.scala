package uk.gov.bis.taxserviceMock.data

import scala.concurrent.{ExecutionContext, Future}

case class AuthRequest(scope: String, clientId: String, redirectUri: String, state: Option[String], id: Long= 0, creationDate: MongoDate = System.currentTimeMillis())

trait AuthRequestOps {
  /**
    * @return a generated identifier for the authRequest record
    */
  def stash(authRequest: AuthRequest)(implicit ec: ExecutionContext): Future[Long]

  def get(id: Long)(implicit ec: ExecutionContext): Future[Option[AuthRequest]]

  def pop(id: Long)(implicit ec: ExecutionContext): Future[Option[AuthRequest]]
}
