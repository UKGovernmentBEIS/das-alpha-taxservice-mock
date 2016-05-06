package uk.gov.bis.taxserviceMock.db

import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

trait SlickModule extends HasDatabaseConfigProvider[JdbcProfile] {
  val api =  driver.api
  import api._

  def run[R, S <: NoStream, E <: Effect](action: DBIOAction[R, S, E]): Future[R] = db.run(action)
}
