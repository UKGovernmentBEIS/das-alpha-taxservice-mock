package uk.gov.bis.taxserviceMock.mongo

import javax.inject._

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import uk.gov.bis.taxserviceMock.data.{AuthId, AuthIdOps}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class AuthIdMongo @Inject()(val mongodb: ReactiveMongoApi) extends MongoCollection[AuthId] with AuthIdOps {
  override val collectionName = "auth_id"

  implicit val authIdF = Json.format[AuthId]

  override def stash(authId: AuthId)(implicit ec: ExecutionContext): Future[Long] = {
    val id = Random.nextLong().abs
    for {
      collection <- collectionF
      r <- collection.insert(authId.copy(id = Some(id)))
    } yield id
  }

  override def pop(id: Long)(implicit ec: ExecutionContext): Future[Option[AuthId]] = {
    findOne("id" -> id).map {
      _.map { d => remove("id" -> id); d }
    }
  }

  override def get(id: Long)(implicit ec: ExecutionContext): Future[Option[AuthId]] = findOne("id" -> id)
}
