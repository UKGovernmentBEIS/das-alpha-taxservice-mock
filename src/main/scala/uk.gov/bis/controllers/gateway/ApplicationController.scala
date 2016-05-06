package uk.gov.bis.controllers.gateway

import play.api.mvc.{Action, Controller}


class ApplicationController extends Controller {
  def index = Action { _ => Redirect(uk.gov.bis.controllers.admin.routes.AdminController.index()) }
}
