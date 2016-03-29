package config

import javax.inject.{Inject, Singleton}

import play.api.Configuration

@Singleton
class ServiceConfig @Inject()(config:Configuration){
  val apiServerEndpointUri = config.getString("api.baseURI").get

}
