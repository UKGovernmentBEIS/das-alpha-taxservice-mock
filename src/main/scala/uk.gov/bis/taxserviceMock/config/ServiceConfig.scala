package uk.gov.bis.taxserviceMock.config

case class Config(api: ApiConfig)

case class ApiConfig(host: String)

object ServiceConfig {

  import pureconfig._

  lazy val config = loadConfig[Config].get
}
