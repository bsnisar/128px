package job

import java.nio.file.Paths

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

//noinspection TypeAnnotation
object ImportEmbeddingsJob extends App  {
  implicit val as = ActorSystem()
  implicit val m = ActorMaterializer()

  val log = LoggerFactory.getLogger("import-job")

  val ps = new PhotoStock(new ImgAssetsDefault(), AhcWSClient())

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

      ps.add(cmd).recover {
        case NonFatal(e) => log.error(s"Error; id=${guid}",e)
      }
    })
    //.log("import")
    .run()

//  System.exit(0)
}
