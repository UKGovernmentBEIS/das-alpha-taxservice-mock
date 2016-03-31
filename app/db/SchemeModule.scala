package db

import java.sql.Date
import javax.inject.{Inject, Singleton}

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

case class SchemeRow(empref: String, terminationDate: Option[Date])


trait SchemeModule extends DBModule {

  import driver.api._

  val Schemes = TableQuery[SchemeTable]

  def insert(cat: SchemeRow): Future[Unit] = db.run(Schemes += cat).map { _ => () }

  class SchemeTable(tag: Tag) extends Table[SchemeRow](tag, "scheme") {

    def empref = column[String]("empref", O.PrimaryKey)

    def terminationDate = column[Option[Date]]("termination_date")


    def * = (empref, terminationDate) <>(SchemeRow.tupled, SchemeRow.unapply)
  }

}

@Singleton
class SchemeDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit val ec: ExecutionContext) extends SchemeModule