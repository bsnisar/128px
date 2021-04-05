package job

import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.csv.scaladsl._
import akka.stream.scaladsl._
import org.slf4j.LoggerFactory
import play.api.libs.ws.ahc.AhcWSClient
import vespa._
import vespa.models.FeedCommand.FeedEntry
import vespa.models.FeedCommand.FeedEntry._
import vespa.models.{FeedCommand, Stash, StockCommandUpdate, TextHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

//noinspection TypeAnnotation
object ImportTagsJob extends App {
  implicit val as = ActorSystem()
  implicit val m = ActorMaterializer()

  val log = LoggerFactory.getLogger("import job")
  implicit val aClient = AhcWSClient()
  val ps = new PhotoStock(new ImgAssetsDefault(), aClient)

  val tsv = s"${sys.props("user.home")}/unsplash/lite/keywords.tsv000"

  // println(ParaStash.identity.hash("--2IBUMom1I"))

  val counter = new AtomicInteger()

  // photo_id        collection_id   collection_title        photo_collected_at
  //--2IBUMom1I     162470  Majestical Sunsets      2016-03-15 17:04:25
  FileIO.fromPath(Paths.get(tsv))
    .via(CsvParsing.lineScanner(delimiter = CsvParsing.Tab))
    .via(CsvToMap.toMap())
    .mapAsyncUnordered(25) {
      dict =>
        val guid = Stash.identity.hash(dict("photo_id").utf8String)
        val keyword = dict("keyword").utf8String.replace("\"","") //cleanup, if any
        val aiConf1 = dict.get("ai_service_1_confidence").filter(_.nonEmpty).map(_.utf8String.toFloat).getOrElse(0f)
        val byUser = dict.get("suggested_by_user").map(_.utf8String).orNull

        val cmd = StockCommandUpdate(
          guid = guid,
          fields = Seq(
            FeedEntry.assignEntry(field = "keywords",
              entryKey = keyword,
              values = Map(
                "suggestedByUser" -> (if (byUser != null && byUser == "f") 1 else 0),
                "aiServiceConfidence1" -> aiConf1
              ))
          )
        )
        ps.add(cmd).recover {
          case NonFatal(e) => log.error(s"Error; photo_id=${dict("photo_id").utf8String}",e)
        }.transform {
          case x@Failure(exception) => x
          case x@Success(value) => if (counter.incrementAndGet() % 200 == 0) { println("next 200..done") }; x
        }
    }
    //.log("job")
    .run()
}
