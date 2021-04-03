package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import vespa.PhotoStock

import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, val stash: PhotoStock)
                              (implicit ec: ExecutionContext) extends BaseController {


  def index() = Action.async { implicit request: Request[AnyContent] =>
    stash.newest().map(h =>
      Ok(views.html.index(h.children))
    )
  }

  def item(id: String) = Action.async { implicit request: Request[AnyContent] =>

    stash.item(id).map(h =>
      h.map { hit => Ok(views.html.item(hit))  } getOrElse NotFound
    )
  }
}
