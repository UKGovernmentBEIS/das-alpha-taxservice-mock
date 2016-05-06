import com.google.inject.AbstractModule
import uk.gov.bis.db.gateway.GatewayEnrolmentModule
import uk.gov.bis.playslicks.GatewayEnrolments

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[GatewayEnrolmentModule]).to(classOf[GatewayEnrolments])
  }

}
