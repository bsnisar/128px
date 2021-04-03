package vespa

import akka.Done
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object BinaryContent {
  case class BinaryContentUriFetchException(m: String) extends RuntimeException(m)
}

sealed trait BinaryContent {
  def stream: Future[Source[ByteString, _]]
}

case class BinaryContentUri(uri: String, data: () => Future[Source[ByteString, _]]) extends BinaryContent {
  def stream: Future[Source[ByteString, _]] = data()
}

object BinaryContentUri {

  def create(uri: String)(implicit ws: WSClient): BinaryContentUri = {
    val stream = () => for {
      response <- ws.url(uri).withMethod("GET").stream()
      _ <- response.status match {
        case 200 =>
          Future.successful(Done)
        case _ => Future.failed(BinaryContent.BinaryContentUriFetchException(
          s"[http response] ${response.uri} ${response.statusText}"))
      }
    } yield response.bodyAsSource

    BinaryContentUri(uri, stream)
  }
}

