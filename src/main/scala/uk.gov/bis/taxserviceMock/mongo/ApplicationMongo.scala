package uk.gov.bis.taxserviceMock.mongo

import javax.inject.Inject

import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import uk.gov.bis.taxserviceMock.data.{ApplicationOps, AuthCodeRow}

import scala.concurrent.{ExecutionContext, Future}

class ApplicationMongo @Inject()(val mongodb: ReactiveMongoApi) extends ApplicationOps {
  implicit val fmt = Json.format[AuthCodeRow]

  def collectionF(implicit ec: ExecutionContext): Future[JSONCollection] = mongodb.database.map(_.collection[JSONCollection]("applications"))

  override def validate(id: String, secret: Option[String], grantType: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    for {
      coll <- collectionF
      o <- coll.find(Json.obj("clientID" -> id, "clientSecret" -> secret)).cursor[JsObject]().collect[List](1).map(_.nonEmpty)
    } yield o
  }
}
