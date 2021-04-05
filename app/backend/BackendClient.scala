package backend

import javax.inject.Inject
import models.Hits.Photos
import play.api.libs.json.{JsObject, JsValue, Json, Reads}
import play.api.libs.ws.{WSClient, WSResponse}
import models.{Embedding, Hits, Photo}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

trait BackendClient {
  def photo(id: String): Future[Option[Photo]]
  def similar(id: Embedding): Future[Photos]
  def brows(limit: Int, offset: Int): Future[Photos]
  def search(limit: Int = 45, offset: Int, search: String): Future[Photos]
}


class VespaBackendClient @Inject() (ws: WSClient)(implicit d: ExecutionContext) extends BackendClient {
  private val vespaUrl = "http://localhost:8080/search/"

  override def photo(id: String): Future[Option[Photo]] = {
    val props = Json.obj("queryProfile" -> "item",
      "presentation.summary" -> "base_view_with_embedding",
      "guid" -> id
    )
    execute[Photo](props).map(_.children.headOption.map(_.value))
  }

  override def search(limit: Int, offset: Int, search: String): Future[Photos] = {
    val props = Json.obj(
      "queryProfile" -> "search",
      "hits" -> limit, "offset" -> offset,
      "ranking.profile" -> "newest",
      "presentation.summary" -> "base_view",
      "searchTerms" -> search
    )

    execute[Photo](props)
  }

  override def brows(limit: Int, offset: Int): Future[Photos] = {
    val props = Json.obj(
      "queryProfile" -> "main",
      "hits" -> limit, "offset" -> offset,
      "ranking.profile" -> "newest",
      "presentation.summary" -> "base_view"
    )

    execute[Photo](props)
  }



  override def similar(p: Embedding): Future[Photos] = {
    val data = Json.obj(
      "yql" -> "select * from stash where [{\"targetHits\": 45}]nearestNeighbor(imgEmbedding, img_embed_param);",
      "ranking.features.query(img_embed_param)" -> (p.tokens.mkString("[", ",", "]")),
      "hits" -> 45,
      "ranking.profile" -> "ann_rank",
      "presentation.summary" -> "base_view"
    )

    execute[Photo](data)

  }

  private def execute[X: Reads](props: JsValue): Future[Hits[X]] = {
    ws.url(vespaUrl).withRequestTimeout(10.seconds).post(props).flatMap(resp => {
      if (resp.status != 200) {
        val prof = props match {
          case JsObject(fields) if fields.contains("queryProfile") => fields.get("queryProfile").map(_.toString())
          case _ => None
        }
        Future.failed(VespaHttpClientException(resp, prof))
      }
      else Future.successful(resp.json.as[Hits[X]])
    })
  }
}


case class VespaHttpClientException(r: WSResponse, prof: Option[String]=None) extends
  RuntimeException(s"${r.uri} [${r.status} ${r.statusText}] - ${prof.getOrElse("n/a")} - ${r.body}")
