package controllers.admin

import javax.inject.{Inject, Singleton}

import db.oauth2.{ClientDAO, ClientRow}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

case class AddAppFormData(id: String, secret: String)

@Singleton
class AdminController @Inject()(clients: ClientDAO)(implicit ec: ExecutionContext) extends Controller {
  def index() = Action {
    Ok(views.html.admin.index())
  }

  def applications = Action.async { implicit request =>
    clients.all().map { cs =>
      Ok(views.html.admin.applications(addAppForm, cs))
    }
  }

  val addAppForm = Form(
    mapping(
      "clientId" -> nonEmptyText,
      "clientSecret" -> nonEmptyText
    )(AddAppFormData.apply)(AddAppFormData.unapply)
  )

  def addApplication = Action.async { implicit request =>
    addAppForm.bindFromRequest().fold(
      errs => clients.all().map(cs => BadRequest(views.html.admin.applications(errs, cs))),
      data => {
        val row = new ClientRow(data.id, Some(data.secret), None, None)
        clients.addClient(row).map { _ =>
          Redirect(controllers.admin.routes.AdminController.applications())
        }
      }
    )
  }

  def removeApplication(id: String) = Action.async { implicit request =>
    clients.remove(id).map(_ => Redirect(controllers.admin.routes.AdminController.applications()))
  }
}
