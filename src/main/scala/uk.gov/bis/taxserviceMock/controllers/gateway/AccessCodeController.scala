package uk.gov.bis.taxserviceMock.controllers.gateway

import javax.inject.Inject

import play.api.mvc.{Action, Controller}
import uk.gov.bis.taxserviceMock.actions.gateway.GatewayUserAction

class AccessCodeController @Inject()(UserAction: GatewayUserAction) extends Controller {

  def show = Action { implicit request =>
    Ok(views.html.gateway.accesscode())
  }

  def handleAccessCode = Action { implicit request =>

    val r = for {
      userId <- request.session.get(UserAction.validatedUserKey)
      uri <- request.session.get(UserAction.continueKey)
    } yield Redirect(uri)
      .removingFromSession(UserAction.continueKey)
      .removingFromSession(UserAction.validatedUserKey)
      .addingToSession((UserAction.sessionKey, userId))

    r.getOrElse(Unauthorized)

  }
}
