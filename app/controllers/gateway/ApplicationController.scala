package controllers.gateway

import play.api.mvc.{Action, Controller}


class ApplicationController extends Controller {
  def index = Action { _ => Ok(views.html.gateway.index()) }
}
