package backend

import java.net.URL

import akka.actor.ActorSystem
import javax.inject.Inject
import models.Embedding
import play.api.libs.ws.WSClient
import play.api.libs.Files
import akka.stream.scaladsl._
import akka.stream.Materializer
import mlops.ImageEmbeddings
import mlops.CalculateEmbeddingRequest

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


trait MLOpsClient {

  /**
   * Resolve image embedding in runtime
   * @param img image url to fetch
   * @return embedding
   */
  def encode(img: URL): Future[Embedding]
}


class MLOpsClientImpl @Inject() (ws: WSClient, tmp: Files.TemporaryFileCreator, gateway: ImageEmbeddings)
                                (implicit ec: ExecutionContext, m: Materializer, as: ActorSystem) extends MLOpsClient {

  private lazy val ioTasks = as.dispatchers.lookup("contexts.img-io-ops")

  /**
   * Resolve image embedding in runtime
   *
   * @param img image url to fetch
   * @return embedding
   */
  override def encode(img: URL): Future[Embedding] = {
    val aTmpFile = tmp.create("img", ".tmp.data")
    for {
      response <- ws.url(img.toString).get()
      io <- response.bodyAsSource.runWith(FileIO.toPath(aTmpFile.path))
      embResp <- io.status match {
        case Failure(exception) => Future.failed(exception)
        case Success(_) => Future {
          gateway.image(CalculateEmbeddingRequest(aTmpFile.path.toUri.toURL))
        }(ioTasks).transform(_.flatten)
      }
    } yield Embedding(embResp.value)
  }
}