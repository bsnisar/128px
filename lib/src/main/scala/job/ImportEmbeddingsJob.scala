package job

import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory
import play.api.libs.ws.ahc.AhcWSClient
import vespa.{ImgAssetsDefault, PhotoStock}
import akka.stream.alpakka.csv.scaladsl._
import akka.stream.scaladsl._
import akka.stream.scaladsl._
import vespa.models.FeedCommand.FeedEntry
import vespa.models.{Embedding, StockCommandUpdate}

import scala.util.control.NonFatal
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

//noinspection TypeAnnotation
object ImportEmbeddingsJob extends App  {
  implicit val as = ActorSystem()
  implicit val m = ActorMaterializer()

  val log = LoggerFactory.getLogger("import-job")

  val ps = new PhotoStock(new ImgAssetsDefault(), AhcWSClient())

  val count = new AtomicLong()

  FileIO.fromPath(Paths.get(s"${sys.props("user.home")}/unsplash/embeddings.csv"))
    .via(CsvParsing.lineScanner(delimiter = CsvParsing.Comma))
    .via(CsvToMap.toMap())
    .throttle(64, 2.seconds)
    .mapAsyncUnordered(8)(dict => {
      val guid = dict("id").utf8String
      val array = dict("vec").utf8String

      val emb = Embedding.fromJson(array)
      val cmd = StockCommandUpdate(
        guid = guid,
        fields = Seq(
          FeedEntry.tensor("imgEmbedding", emb.get)
        )
      )

      val done = ps.add(cmd).recover {
        case NonFatal(e) => log.error(s"Error; id=${guid}",e)
      }

      done.onComplete {
        case Success(_) if count.incrementAndGet() % 700 == 0 =>
          println("process...next 700")
        case _ => ()
      }

      done
    })
    //.log("import")
    .run()

//  System.exit(0)
}
