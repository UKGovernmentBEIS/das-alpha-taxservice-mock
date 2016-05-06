package uk.gov.bis.taxserviceMock.controllers.gateway

import play.api.mvc.{Action, Controller}


class ApplicationController extends Controller {
  def index = Action { _ => Redirect(uk.gov.bis.taxserviceMock.controllers.admin.routes.AdminController.index()) }
}
