package uk.gov.bis.playslicks

import javax.inject.Inject

import uk.gov.bis.db.SchemeModule
import uk.gov.bis.db.gateway.{GatewayEnrolmentModule, GatewayIdModule}
import play.api.db.slick.DatabaseConfigProvider

class GatewayEnrolments @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends GatewayEnrolmentModule
    with GatewayIdModule
    with SchemeModule
