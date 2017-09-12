package uk.gov.bis.taxserviceMock.data

import scala.concurrent.{ExecutionContext, Future}

case class Scope(name: String, description: String, needsExplicitGrant: Option[Boolean])

trait ScopeOps {
  def byName(scopeName: String)(implicit ec: ExecutionContext): Future[Option[Scope]]
}
