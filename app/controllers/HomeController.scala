package controllers

import java.net.URL

import backend.BackendClient
import javax.inject._
import models.SimilarPhotosView
import play.api._
import play.api.i18n.{Lang, Messages}
import play.api.mvc._
import play.filters.csrf.CSRF
import vespa.PhotoStock

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val backendClient: BackendClient,
                               val stash: PhotoStock)
                              (implicit ec: ExecutionContext) extends BaseController {

  implicit val messages: Messages = messagesApi.preferred(Seq(Lang.defaultLang))


  def search: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    MyForms.searchForm.bindFromRequest.fold(
      formWithErrors => {
        // binding failure, you retrieve the form containing errors:
        Future.successful(BadRequest("nope"))
      },
      userData => {

        backendClient.search(offset = 0, search = userData.value).map(h =>
          Ok(views.html.searchPage(h.children, userData.value
          ))
        )

      }
    )
  }

  def index(offset: Option[Int]): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>

    backendClient.brows(45, offset.getOrElse(0)).map(h =>
      Ok(views.html.index(h.children))
    )
  }

  def item(id: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    for {
      photo <- backendClient.photo(id)
      moreLike <- unwrap(for {
        p <- photo
        emb <- p.emb
      } yield backendClient.similar(emb))
    } yield (photo, moreLike) match {
      case (Some(photo), Some(similar)) =>
        val view = SimilarPhotosView(photo, similar.children.map(_.value))
        Ok(views.html.item(view))
      case (Some(photo), None) =>
        val view = SimilarPhotosView(photo)
        Ok(views.html.item(view))
      case _ => NotFound
    }
  }

  def unwrap[A](x: Option[Future[A]])(implicit ec: ExecutionContext): Future[Option[A]] =
    x match {
      case Some(f) => f.map(Some(_))
      case None    => Future.successful(None)
    }


  def byImage: Action[AnyContent] = Action { implicit request: Request[_] =>
    Ok(views.html.findByImage())
  }

  def searchByImage: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    MyForms.searchByUrlForm.bindFromRequest.fold(
      formWithErrors => {
        // binding failure, you retrieve the form containing errors:
        Future.successful(BadRequest("nope"))
      },
      userData => {
        backendClient.similarByExample(new URL(userData.url)).map(h =>
          Ok(views.html.index(h.children))
        )
      }
    )
  }
}
