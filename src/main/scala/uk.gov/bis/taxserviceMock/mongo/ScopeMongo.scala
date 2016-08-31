package uk.gov.bis.taxserviceMock.mongo

import javax.inject.Inject

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import uk.gov.bis.taxserviceMock.data.{Scope, ScopeOps}

import scala.concurrent.ExecutionContext

class ScopeMongo @Inject()(val mongodb: ReactiveMongoApi) extends MongoCollection[Scope] with ScopeOps {
  implicit val scopeR = Json.reads[Scope]

  override def collectionName: String = "scopes"

  override def byName(scopeName: String)(implicit ec: ExecutionContext) = findOne("name" -> scopeName)
}
