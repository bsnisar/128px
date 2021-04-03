package job

import java.nio.file.Paths
import java.time.format._
import java.time.temporal._
import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.csv.scaladsl._
import akka.stream.scaladsl._
import vespa._
import vespa.models.{MetadataUnsplash, Stash, StockCommandAdd}
import org.slf4j.LoggerFactory
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

//noinspection TypeAnnotation
object ImportJob extends App {
  implicit val as = ActorSystem()
  implicit val m = ActorMaterializer()

  val log = LoggerFactory.getLogger("import job")
  implicit val aClient = AhcWSClient()
  val ps = new PhotoStock(new ImgAssetsDefault(), aClient)

  val tsv = s"${sys.props("user.home")}/unsplash/lite/photos.tsv000"

  private val fmt = new DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd HH:mm:ss")
    .optionalStart()
    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
    .toFormatter()

  log.info("Start")

  FileIO.fromPath(Paths.get(tsv))
    .via(CsvParsing.lineScanner(delimiter = CsvParsing.Tab))
    .via(CsvToMap.toMap())
    .mapAsyncUnordered(18) {
      dict =>
        val id = dict("photo_id").utf8String
        val guid = Stash.identity.hash(id)
        val photoUrl = dict("photo_url").utf8String
        val photoImgUrl = dict("photo_image_url").utf8String
        val photoDescription = dict("photo_description").utf8String
        val time = dict("photo_submitted_at").utf8String
        val createAt = LocalDateTime.parse(time, fmt).toInstant(ZoneOffset.UTC)

        val cmd = StockCommandAdd(
          guid = guid,
          description = photoDescription,
          publishTs = createAt,
          binaryGuid = ImgAssetKey.hashed(id),
          binary = BinaryContentUri.create(photoImgUrl),
          metadata = MetadataUnsplash(id, photoUrl, photoImgUrl),
          download = false
        )

        ps.add(cmd).recover {
          case NonFatal(e) => log.error(s"Error; photo_id=$id",e)
        }
    }
    .run()
}
