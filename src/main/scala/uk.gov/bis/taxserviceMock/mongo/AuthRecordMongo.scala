package uk.gov.bis.taxserviceMock.mongo

import javax.inject._

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import uk.gov.bis.taxserviceMock.data.{AuthRecordOps, AuthRecord}

import scala.concurrent.{ExecutionContext, Future}

class AuthRecordMongo @Inject()(val mongodb: ReactiveMongoApi) extends MongoCollection[AuthRecord] with AuthRecordOps {

  implicit val tokenF = Json.format[AuthRecord]

  override val collectionName = "sys_auth_records"

  override def forRefreshToken(refreshToken: String)(implicit ec: ExecutionContext) = findOne("refreshToken" -> refreshToken)

  override def forAccessToken(accessToken: String)(implicit ec: ExecutionContext) = findOne("accessToken" -> accessToken)

  override def find(gatewayID: String, clientId: Option[String])(implicit ec: ExecutionContext) = findOne("gatewayID" -> gatewayID, "clientID" -> clientId)

  override def create(token: AuthRecord)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      collection <- collectionF
      r <- collection.insert(token)
    } yield ()
  }

  override def deleteExistingAndCreate(token: AuthRecord)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      coll <- collectionF
      _ <- coll.remove(Json.obj("accessToken" -> token.accessToken))
      _ <- coll.insert(token)
    } yield ()
  }
}
