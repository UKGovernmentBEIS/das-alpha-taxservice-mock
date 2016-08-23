import com.google.inject.AbstractModule
import uk.gov.bis.taxserviceMock.data.GatewayUserOps
import uk.gov.bis.taxserviceMock.db.gateway.GatewayEnrolmentModule
import uk.gov.bis.taxserviceMock.mongo.GatewayUserMongo
import uk.gov.bis.taxserviceMock.playslicks.GatewayEnrolments

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[GatewayEnrolmentModule]).to(classOf[GatewayEnrolments])
    bind(classOf[GatewayUserOps]).to(classOf[GatewayUserMongo])
  }

}
