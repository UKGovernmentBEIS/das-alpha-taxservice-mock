package uk.gov.bis.taxserviceMock.config

import javax.inject.{Inject, Singleton}

import play.api.Configuration

@Singleton
class ServiceConfig @Inject()(config:Configuration){
  val apiHost = config.getString("api.host").get
}
