import com.google.inject.AbstractModule
import uk.gov.bis.taxserviceMock.data.{AccessTokenOps, GatewayUserOps}
import uk.gov.bis.taxserviceMock.mongo.{AccessTokenMongo, GatewayUserMongo}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[GatewayUserOps]).to(classOf[GatewayUserMongo])
    bind(classOf[AccessTokenOps]).to(classOf[AccessTokenMongo])
  }

}
