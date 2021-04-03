package vespa

import akka.Done
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.libs.ws._
import vespa.models.{Embedding, FeedCommand, PhotosSimilar, Photo, StockCommandAdd, StockCommandUpdate}
import vespa.api.{Hit, Hits}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


class PhotoStock @Inject()(binaryStorage: ImgAssets, ws: WSClient) {
  import models.Photo._
  import models.Embedding._
  import api.Reads._

  private val vespa = "http://localhost:8080/search/"
  private val vespaFeedApi = "http://localhost:8080/document/v1/default"
  private val stashDocName = "stash"


  /**
   * Get by id.
   * @param id guid
   * @return photo
   */
  def item(id: String): Future[Option[PhotosSimilar]] = {
    val data = Json.obj("queryProfile" -> "item",
      "presentation.summary" -> "base_view_with_embedding",
      "guid" -> id
    )

    for {
      photo <- fetchOne[Photo](data)
      photos <- photo.flatMap(_.emb) match {
        case Some(value) => moreLikeThis(value).map(_.children.map(_.value).toSeq)
        case None => Future.successful(Seq.empty)
      }
    } yield {
      photo.map(p => PhotosSimilar(p, photos))
    }
  }


  private def moreLikeThis(p: Embedding): Future[Hits[Photo]] = {
    val data = Json.obj(
      "yql" -> "select * from stash where [{\"targetHits\": 45}]nearestNeighbor(imgEmbedding, img_embed_param);",
      "ranking.features.query(img_embed_param)" -> (p.tokens.mkString("[", ",", "]")),
      "hits" -> 45,
      "ranking.profile" -> "ann_rank",
      "presentation.summary" -> "base_view"
    )
    fetch[Photo](data)
  }

  /**
   * Find newest photos, ranked.
   *
   * @return photos
   */
  def newest(hits: Int = 30, offset: Int = 0): Future[Hits[Photo]] = {
    val data = Json.obj(
      "queryProfile" -> "main",
      "hits" -> hits, "offset" -> offset,
      "ranking.profile" -> "newest",
      "presentation.summary" -> "base_view"
    )

    fetch[Photo](data)
  }


  /**
   * Save new content.
   *
   * @param input input cmd
   * @return when Done
   */
  def add(input: FeedCommand): Future[Done] = {
    def asSuccess(r: WSResponse): Future[Unit] = {
      if (r.status != 200)
        Future.failed(VespaHttpRequestException(r))
      else Future.unit
    }

    input match {
      case add: StockCommandAdd => for {
        _ <- binaryStorage.persist(add.binaryGuid, add.binary, replace = add.download)
        _ <- input.feed.map { feed =>
          ws.url(s"$vespaFeedApi/$stashDocName/docid/${feed.id}").post(feed.json).flatMap(asSuccess)
        }.getOrElse(Future.unit)
      } yield {
        PhotoStock.log.debug("[#add] finished {}", input.uuid)
        Done
      }
      case upd: StockCommandUpdate => upd.feed match {
        case Some(value) => for {
          _ <- ws.url(s"$vespaFeedApi/$stashDocName/docid/${value.id}")
            .withRequestTimeout(8.seconds)
            .put(value.json)
            .flatMap(asSuccess)
        } yield {
          PhotoStock.log.debug("[#update] finished {}", input.uuid)
          Done
        }
        case None =>
          PhotoStock.FutureDone
      }
    }
  }

  private def fetchOne[T:Reads](data: JsValue): Future[Option[T]] = {
    for {
      opt <- fetch[T](data)
    } yield {
      opt.children.headOption.map(hit => hit.value)
    }
  }

  private def fetch[X: Reads](props: JsValue): Future[Hits[X]] = {
    ws.url(vespa).withRequestTimeout(10.seconds).post(props).flatMap(resp => {
      if (resp.status != 200) {
        val prof = props match {
          case JsObject(fields) if fields.contains("queryProfile") => fields.get("queryProfile").map(_.toString())
          case _ => None
        }
        Future.failed(VespaHttpRequestException(resp, prof))
      }
      else Future.successful(resp.json.as[Hits[X]])
    })
  }
}

object PhotoStock {
  val FutureDone: Future[Done] = Future.successful(Done)
  private val log = LoggerFactory.getLogger(classOf[PhotoStock])
}

case class VespaHttpRequestException(r: WSResponse, prof: Option[String]=None) extends
  RuntimeException(s"${r.uri} [${r.status} ${r.statusText}] - ${prof.getOrElse("n/a")} - ${r.body}")

