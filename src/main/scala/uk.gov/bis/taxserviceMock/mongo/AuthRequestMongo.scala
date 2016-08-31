package uk.gov.bis.taxserviceMock.mongo

import javax.inject._

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import uk.gov.bis.taxserviceMock.data.{AuthRequest, AuthRequestOps}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class AuthRequestMongo @Inject()(val mongodb: ReactiveMongoApi) extends MongoCollection[AuthRequest] with AuthRequestOps {
  override val collectionName = "auth_requests"

  implicit val authIdF = Json.format[AuthRequest]

  override def stash(authRequest: AuthRequest)(implicit ec: ExecutionContext): Future[Long] = {
    val id = Random.nextLong().abs
    for {
      collection <- collectionF
      r <- collection.insert(authRequest.copy(id = id))
    } yield id
  }

  override def pop(id: Long)(implicit ec: ExecutionContext): Future[Option[AuthRequest]] = {
    findOne("id" -> id).map {
      _.map { d => remove("id" -> id); d }
    }
  }

  override def get(id: Long)(implicit ec: ExecutionContext): Future[Option[AuthRequest]] = findOne("id" -> id)
}
