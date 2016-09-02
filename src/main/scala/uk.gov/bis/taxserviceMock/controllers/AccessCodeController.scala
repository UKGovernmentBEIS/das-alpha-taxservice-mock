package uk.gov.bis.taxserviceMock.controllers

import javax.inject.Inject

import play.api.mvc.Controller
import uk.gov.bis.taxserviceMock.actions.GatewayUserAction

class AccessCodeController @Inject()(UserAction: GatewayUserAction) extends Controller {
  def show(continue: String, origin: Option[String]) = UserAction { implicit request =>
    Ok(views.html.accesscode(continue, origin))
  }

  def handleAccessCode(continue: String, origin: Option[String]) = UserAction { implicit request =>
    Redirect(continue)
  }
}
