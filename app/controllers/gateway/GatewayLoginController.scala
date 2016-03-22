package controllers.gateway

import javax.inject.{Singleton, Inject}

import actions.gateway.GatewayUserAction
import db.gateway.GatewayUserDAO
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}

case class UserData(name: String, password: String)

@Singleton
class GatewayLoginController @Inject()(gatewayUserDAO: GatewayUserDAO, UserAction: GatewayUserAction)(implicit exec: ExecutionContext) extends Controller {

  val userForm = Form(
    mapping(
      "username" -> text,
      "password" -> text
    )(UserData.apply)(UserData.unapply)
  )

  def showLogin = Action {
    Ok(views.html.gateway.login(userForm))
  }

  def handleLogin = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.gateway.login(formWithErrors))),
      userData => {
        gatewayUserDAO.validate(userData.name, userData.password).map {
          case Some(user) =>
            request.session.get("uri") match {
              case Some(uri) => Redirect(uri).removingFromSession("uri").addingToSession((UserAction.sessionKey, user.id.toString))
              case None => Redirect(controllers.gateway.routes.ApplicationController.index()).addingToSession(UserAction.sessionKey -> user.id.toString)
            }
          case None => Ok(views.html.gateway.login(userForm.withError("username", "Bad user name or password")))
        }
      }
    )
  }

  def logout = Action {
    Redirect(controllers.gateway.routes.GatewayLoginController.showLogin()).withNewSession
  }
}
