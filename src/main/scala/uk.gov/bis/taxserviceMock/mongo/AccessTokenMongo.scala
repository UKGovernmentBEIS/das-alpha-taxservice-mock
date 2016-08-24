package uk.gov.bis.taxserviceMock.mongo

import javax.inject._

import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import uk.gov.bis.taxserviceMock.data.{AccessTokenOps, AccessTokenRow}

import scala.concurrent.{ExecutionContext, Future}

class AccessTokenMongo @Inject()(val mongodb: ReactiveMongoApi) extends AccessTokenOps {

  implicit val tokenF = Json.format[AccessTokenRow]

  def collectionF(implicit ec: ExecutionContext): Future[JSONCollection] = mongodb.database.map(_.collection[JSONCollection]("auth_records"))

  override def forRefreshToken(refreshToken: String)(implicit ec: ExecutionContext): Future[Option[AccessTokenRow]] = {
    val of = for {
      collection <- collectionF
      o <- collection.find(Json.obj("refreshToken" -> refreshToken)).cursor[JsObject]().collect[List](1).map(_.headOption)
    } yield o

    of.map {
      case Some(o) => o.validate[AccessTokenRow].asOpt
      case _ => None
    }
  }

  override def forAccessToken(accessToken: String)(implicit ec: ExecutionContext): Future[Option[AccessTokenRow]] =  {
    val of = for {
      collection <- collectionF
      o <- collection.find(Json.obj("accessToken" -> accessToken)).cursor[JsObject]().collect[List](1).map(_.headOption)
    } yield o

    of.map {
      case Some(o) => o.validate[AccessTokenRow].asOpt
      case _ => None
    }
  }

  override def find(gatewayID: String, clientId: Option[String])(implicit ec: ExecutionContext): Future[Option[AccessTokenRow]] =  {
    val of = for {
      collection <- collectionF
      o <- collection.find(Json.obj("gatewayID" -> gatewayID, "clientID" -> clientId)).cursor[JsObject]().collect[List](1).map(_.headOption)
    } yield o

    of.map {
      case Some(o) => o.validate[AccessTokenRow].asOpt
      case _ => None
    }
  }

  override def create(token: AccessTokenRow)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      collection <- collectionF
      r <- collection.insert(token)
    } yield ()
  }

  override def deleteExistingAndCreate(token: AccessTokenRow)(implicit ec: ExecutionContext): Future[Unit] = ???
}
