package controllers.gateway

import play.api.mvc.{Action, Controller}


class ApplicationController extends Controller {
  def index = Action { _ => Redirect(controllers.admin.routes.AdminController.index()) }
}
