package controllers.gateway

import javax.inject.{Singleton, Inject}

import actions.gateway.GatewayUserAction
import db.gateway.GatewayUserDAO
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}

case class UserData(userId: String, password: String)

@Singleton
class GatewaySignInController @Inject()(gatewayUserDAO: GatewayUserDAO, UserAction: GatewayUserAction)(implicit exec: ExecutionContext) extends Controller {

  val userForm = Form(
    mapping(
      "userId" -> text,
      "password" -> text
    )(UserData.apply)(UserData.unapply)
  )

  def showSignIn = Action {
    Ok(views.html.gateway.signIn(userForm))
  }

  def handleSignIn = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.gateway.signIn(formWithErrors))),
      userData => {
        gatewayUserDAO.validate(userData.userId, userData.password).map {
          case Some(user) =>
            request.session.get(UserAction.continueKey) match {
              case Some(uri) => Redirect(uri).removingFromSession(UserAction.continueKey).addingToSession((UserAction.sessionKey, user.id.toString))
              case None => Redirect(controllers.gateway.routes.ApplicationController.index()).addingToSession(UserAction.sessionKey -> user.id.toString)
            }
          case None => Ok(views.html.gateway.signIn(userForm.withError("username", "Bad user name or password")))
        }
      }
    )
  }

  def signOut = Action {
    Redirect(controllers.gateway.routes.GatewaySignInController.showSignIn()).withNewSession
  }
}
