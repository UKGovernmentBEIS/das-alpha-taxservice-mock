package uk.gov.bis.taxserviceMock.mongo

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import uk.gov.bis.taxserviceMock.data.{AuthCodeOps, AuthCodeRow}

import scala.concurrent.{ExecutionContext, Future}

class AuthCodeMongo @Inject()(val mongodb: ReactiveMongoApi) extends MongoCollection[AuthCodeRow] with AuthCodeOps {
  implicit val fmt = Json.format[AuthCodeRow]

  override val collectionName: String = "auth_codes"

  override def find(code: String)(implicit ec: ExecutionContext) = findOne("authorizationCode" -> code)

  override def delete(code: String)(implicit ec: ExecutionContext): Future[Int] = {
    for {
      coll <- collectionF
      i <- coll.remove(Json.obj("authorizationCode" -> code))
    } yield i.n
  }

  override def create(code: String, gatewayUserId: String, redirectUri: String, clientId: String, scope: String)(implicit ec: ExecutionContext): Future[Int] = {
    Logger.debug("create auth code entry")
    val row = AuthCodeRow(code, gatewayUserId, redirectUri, System.currentTimeMillis(), Some("read:apprenticeship-levy"), Some(clientId), 3600)
    for {
      coll <- collectionF
      i <- coll.insert(row)
    } yield i.n
  }
}
