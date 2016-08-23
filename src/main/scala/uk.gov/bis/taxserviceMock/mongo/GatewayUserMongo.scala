package uk.gov.bis.taxserviceMock.mongo

import javax.inject._

import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo._
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import uk.gov.bis.taxserviceMock.data.{GatewayUser, GatewayUserOps}

import scala.concurrent.{ExecutionContext, Future}

class GatewayUserMongo @Inject()(val mongodb: ReactiveMongoApi) extends GatewayUserOps {

  implicit val userR = Json.reads[GatewayUser]

  def collectionF(implicit ec: ExecutionContext): Future[JSONCollection] = mongodb.database.map(_.collection[JSONCollection]("gateway_users"))

  override def forGatewayID(gatewayId: String)(implicit ec: ExecutionContext): Future[Option[GatewayUser]] = {
    val of = for {
      collection <- collectionF
      o <- collection.find(Json.obj("gatewayID" -> gatewayId)).cursor[JsObject]().collect[List](1).map(_.headOption)
    } yield o

    of.map {
      case Some(o) => o.validate[GatewayUser].asOpt
      case _ => None
    }
  }

  override def forEmpref(empref: String)(implicit ec: ExecutionContext): Future[Option[GatewayUser]] = {
    val of = for {
      collection <- collectionF
      o <- collection.find(Json.obj("empref" -> empref)).cursor[JsObject]().collect[List](1).map(_.headOption)
    } yield o

    of.map {
      case Some(o) => o.validate[GatewayUser].asOpt
      case _ => None
    }
  }

  override def validate(gatewayID: String, password: String)(implicit ec: ExecutionContext): Future[Option[GatewayUser]] = {
    val of = for {
      collection <- collectionF
      o <- collection.find(Json.obj("gatewayID" -> gatewayID, "password" -> password)).cursor[JsObject]().collect[List](1).map(_.headOption)
    } yield o

    of.map {
      case Some(o) => o.validate[GatewayUser].asOpt
      case _ => None
    }
  }
}
