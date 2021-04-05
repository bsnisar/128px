package vespa

import java.io.{File, InputStream}
import java.nio.file.{Path, Paths, Files => JFiles}
import java.time.Instant
import java.util.Base64

import akka.Done
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source, StreamConverters}
import akka.util.ByteString
import com.google.common.base.{Charsets, Strings}
import com.google.common.hash.Hashing

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


trait ImgAssets {

  /**
   * GET
   */
  def get(key: ImgAssetPath): Future[Option[ImgAsset]]

  /**
   * SAVE
   */
  def persist(key: ImgAssetKey, content: BinaryContent, replace: Boolean = true): Future[_]
}


class ImgAssetsDefault(partitioner: IPartitioner = new PartitionerDefault,
                       dir: Path = Paths.get(sys.props("user.home"),"/.stash/caches"))
                      (implicit m: Materializer, as: ActorSystem) extends ImgAssets {

  private val mainImageFile = "index.jpeg"
  private val w356ImageFile = "index-w-640.jpeg"

  private implicit lazy val expensiveIoLookups: ExecutionContext =
    as.dispatchers.lookup("contexts.img-io-ops")

  /**
   * GET
   *
   * @return
   */
  override def get(r: ImgAssetPath): Future[Option[ImgAsset]] = {
    val destObject: Path = resolve(r.key)

    val resolvedFile = r.size match {
      case Some(ImageKind.M) => destObject.resolve(w356ImageFile)
      case _ => destObject.resolve(mainImageFile)
    }

    def access(aPath: Path): Option[ImgAsset] = aPath match {
      case path if JFiles.exists(path) => Some(ImgAssetFile(path.toFile))
      case _ => None
    }

    Future.successful(access(resolvedFile))
  }

  /**
   * Save next binary object.
   *
   * @param key     object id
   * @param content content
   * @return resulting effect
   */
  override def persist(key: ImgAssetKey, content: BinaryContent, replace: Boolean = true): Future[_] = {
    val destObject: Path = resolve(key)
    val dest: Path = destObject.resolve(mainImageFile)
    if (JFiles.exists(dest) && !replace) {
      Future.successful(Done)
    } else {
      for {
        source <- content.stream
        _ <- {
          if (!JFiles.exists(destObject)) {
            JFiles.createDirectories(destObject)
          }
          val fileSink = FileIO.toPath(dest)
          val fileSource = source
          fileSource.runWith(fileSink)
        }
      } yield Done
    }
  }

  private def resolve(key: ImgAssetKey) = {
    val (aShard, aKey) = partitioner.partition(key)
    val dest = dir.resolve(Paths.get(aShard, aKey))
    dest
  }
}

//noinspection TypeAnnotation
object ImageKind extends Enumeration {
  type Size = Value
  val M, Full = Value

  def from(s: String):Option[ImageKind.Size] = Option(s).map(_.trim.toLowerCase) match {
    case Some("m") => Some(M)
    case Some("full") => Some(Full)
    case _ => throw new IllegalArgumentException("unknown enum type " + s)
  }
}

case class ImgAssetPath(key: ImgAssetKey, size: Option[ImageKind.Size] = None)


/**
 * A key.
 *
 * @param value string
 */
class ImgAssetKey private(val value: String)

object ImgAssetKey {

  /**
   * Base64
   *
   * @param s input
   */
  def apply(s: String): ImgAssetKey =
    new ImgAssetKey(s)


  /**
   * Base64
   *
   * @param s input
   */
  def base64(s: String): ImgAssetKey =
    new ImgAssetKey(new String(Base64.getEncoder.encode(s.getBytes(Charsets.UTF_8))))

  /**
   * Hashed id.
   *
   * @param s input
   */
  def hashed(s: String): ImgAssetKey = {
    //noinspection UnstableApiUsage
    val hashCode = Hashing.murmur3_128().hashString(s, Charsets.UTF_8)
    val digest = hashCode
    new ImgAssetKey(s"img-$digest")
  }
}

/**
 * Assets
 */
sealed trait ImgAsset {
  val mimeType: String = "image/jpeg"

  def stream: Source[ByteString, Future[_]]

  def input: Try[InputStream]

  def length: Option[Long]

  def lastModified: Option[Instant]
}

/**
 * Assets from file
 *
 * @param f file
 */
case class ImgAssetFile(f: File) extends ImgAsset {
  override def length: Option[Long] = Some(f.length())

  override def input: Try[InputStream] = Try(JFiles.newInputStream(f.toPath))

  override def stream: Source[ByteString, Future[_]] = StreamConverters.fromInputStream(() => this.input.get)

  override def lastModified: Option[Instant] = Some(Instant.ofEpochMilli(f.lastModified))
}


trait IPartitioner {
  def partition(key: ImgAssetKey): (String, String)
}


class PartitionerDefault extends IPartitioner {

  //noinspection UnstableApiUsage
  override def partition(key: ImgAssetKey): (String, String) = {
    val base = Hashing.murmur3_128().hashString(key.value, Charsets.UTF_8)
    val shard = Hashing.consistentHash(base, 256)
    val `object` = s"o_${key.value}"
    (s"shard_${Strings.padStart(shard.toString, 9, '0')}", `object`)
  }
}


