package playslicks

import javax.inject.Inject

import db.SchemeModule
import db.gateway.{GatewayEnrolmentModule, GatewayIdModule}
import play.api.db.slick.DatabaseConfigProvider

class GatewayEnrolments @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends GatewayEnrolmentModule
    with GatewayIdModule
    with SchemeModule
