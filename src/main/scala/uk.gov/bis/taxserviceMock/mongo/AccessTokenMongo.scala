package uk.gov.bis.taxserviceMock.mongo

import javax.inject._

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import uk.gov.bis.taxserviceMock.data.{AccessTokenOps, AccessTokenRow}

import scala.concurrent.{ExecutionContext, Future}

class AccessTokenMongo @Inject()(val mongodb: ReactiveMongoApi) extends MongoCollection[AccessTokenRow] with AccessTokenOps {

  implicit val tokenF = Json.format[AccessTokenRow]

  override val collectionName = "auth_records"

  override def forRefreshToken(refreshToken: String)(implicit ec: ExecutionContext) = findOne("refreshToken" -> refreshToken)

  override def forAccessToken(accessToken: String)(implicit ec: ExecutionContext) = findOne("accessToken" -> accessToken)

  override def find(gatewayID: String, clientId: Option[String])(implicit ec: ExecutionContext) = findOne("gatewayID" -> gatewayID, "clientID" -> clientId)

  override def create(token: AccessTokenRow)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      collection <- collectionF
      r <- collection.insert(token)
    } yield ()
  }

  override def deleteExistingAndCreate(token: AccessTokenRow)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      coll <- collectionF
      _ <- coll.remove(Json.obj("accessToken" -> token.accessToken))
      _ <- coll.insert(token)
    } yield ()
  }
}
