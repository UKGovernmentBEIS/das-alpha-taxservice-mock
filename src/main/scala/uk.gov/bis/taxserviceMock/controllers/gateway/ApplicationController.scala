package uk.gov.bis.taxserviceMock.controllers.gateway

import javax.inject.Inject

import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

class ApplicationController @Inject()(implicit exec: ExecutionContext) extends Controller {
  def index = Action { _ => Redirect(uk.gov.bis.taxserviceMock.controllers.admin.routes.AdminController.index()) }
}
