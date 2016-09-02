package uk.gov.bis.taxserviceMock.controllers

import javax.inject.{Inject, Singleton}

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import uk.gov.bis.taxserviceMock.actions.GatewayUserAction
import uk.gov.bis.taxserviceMock.data.GatewayUserOps

import scala.concurrent.{ExecutionContext, Future}

case class UserData(userId: String, password: String)

@Singleton
class GatewaySignInController @Inject()(gatewayUsers: GatewayUserOps, UserAction: GatewayUserAction)(implicit exec: ExecutionContext) extends Controller {

  val userForm = Form(
    mapping(
      "userId" -> nonEmptyText,
      "password" -> nonEmptyText
    )(UserData.apply)(UserData.unapply)
  )

  def show(continue: String, origin: Option[String]) = Action {
    Ok(views.html.signIn(continue, origin, userForm))
  }

  def handleSignIn(continue: String, origin: Option[String]) = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.signIn(continue, origin, formWithErrors))),
      userData => {
        gatewayUsers.validate(userData.userId, userData.password).map {
          case Some(user) =>
            if (user.require2SV.getOrElse(false))
              Redirect(routes.AccessCodeController.show(continue, origin)).addingToSession((UserAction.validatedUserKey, user.gatewayID))
            else
              Redirect(continue).addingToSession((UserAction.validatedUserKey, user.gatewayID))

          case None => Ok(views.html.signIn(continue, origin, userForm.withError("username", "Bad user name or password")))
        }
      }
    )
  }

}
