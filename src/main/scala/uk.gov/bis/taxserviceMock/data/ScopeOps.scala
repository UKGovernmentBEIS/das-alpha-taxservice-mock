package uk.gov.bis.taxserviceMock.data

import scala.concurrent.{ExecutionContext, Future}

case class Scope(name: String, description: String, needsExplicitGrant: Option[Boolean])

trait ScopeOps {
  def byName(scopeName: String)(implicit ec: ExecutionContext): Future[Option[Scope]]

  /**
    * Return all of the `Scope` identified by the names, or a list of the scope names that were
    * not valid.
    *
    * @return
    */
  def byNames(names: Seq[String])(implicit ec: ExecutionContext): Future[Either[Seq[String], Seq[Scope]]]
}
