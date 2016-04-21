import com.google.inject.AbstractModule
import db.gateway.GatewayEnrolmentModule
import playslicks.GatewayEnrolments

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[GatewayEnrolmentModule]).to(classOf[GatewayEnrolments])
  }

}
