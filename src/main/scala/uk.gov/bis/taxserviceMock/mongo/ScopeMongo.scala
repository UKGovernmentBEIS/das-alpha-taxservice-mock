package uk.gov.bis.taxserviceMock.mongo

import javax.inject.Inject

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import uk.gov.bis.taxserviceMock.data.{Scope, ScopeOps}
import cats.syntax.traverse._
import cats.instances.list._
import cats.instances.future._

import scala.concurrent.{ExecutionContext, Future}

class ScopeMongo @Inject()(val mongodb: ReactiveMongoApi) extends MongoCollection[Scope] with ScopeOps {
  implicit val scopeR = Json.reads[Scope]

  override def collectionName: String = "sys_scopes"

  override def byName(scopeName: String)(implicit ec: ExecutionContext) = findOne("name" -> scopeName)

  override def byNames(names: Seq[String])(implicit ec: ExecutionContext): Future[Either[Seq[String], Seq[Scope]]] = {
    val resultsF = names.toList.traverse[Future, Either[String, Scope]] { name =>
      byName(name).map {
        case None        => Left(name)
        case Some(scope) => Right(scope)
      }
    }

    resultsF.map { results =>
      val unknownScopes: Seq[String] = results.collect {
        case Left(s) => s
      }

      if (unknownScopes.nonEmpty)
        Left(unknownScopes)
      else
        Right(results.collect { case Right(s) => s })
    }
  }
}
