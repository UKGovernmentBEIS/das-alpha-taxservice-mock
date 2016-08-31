import com.google.inject.AbstractModule
import uk.gov.bis.taxserviceMock.data._
import uk.gov.bis.taxserviceMock.mongo._

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[GatewayUserOps]).to(classOf[GatewayUserMongo])
    bind(classOf[AccessTokenOps]).to(classOf[AccessTokenMongo])
    bind(classOf[AuthCodeOps]).to(classOf[AuthCodeMongo])
    bind(classOf[AuthIdOps]).to(classOf[AuthIdMongo])
    bind(classOf[ClientOps]).to(classOf[ClientMongo])
    bind(classOf[ScopeOps]).to(classOf[ScopeMongo])
  }
}
