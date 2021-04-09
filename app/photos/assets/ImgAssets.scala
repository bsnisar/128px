package photos.assets

import com.google.common.base.{Charsets, Strings}
import com.google.common.hash.Hashing
import java.nio.file.{Path, Paths, Files => JFiles}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import javax.inject.Inject
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

trait ImgAssets {
  /**
   * GET
   */
  def get(key: ImgAssetKey, kind: Option[ImageKind.Size] = None): Future[Option[ImgAsset]]

  /**
   * SAVE
   */
  def persist(key: ImgAssetKey, content: BinaryContent, replace: Boolean = true): Future[_]
}



class ImgAssetsDefault @Inject() (partitioner: IPartitioner = new PartitionerDefault, cfg: Configuration)
                      (implicit m: Materializer, as: ActorSystem, es: ExecutionContext) extends ImgAssets {

  private val dir: Path = cfg.getOptional[String]("binaries.fs-path").map(d => Paths.get(d))
    .getOrElse(Paths.get(sys.props("user.home"),"/.stash/caches"))

  override def get(key: ImgAssetKey, size: Option[ImageKind.Size]): Future[Option[ImgAsset]] = {
    val destObject: Path = resolve(key)

    val resolvedFile = size match {
      case Some(ImageKind.M) => destObject.resolve(ImgAssetsDefault.w640ImageFile)
      case _ => destObject.resolve(ImgAssetsDefault.MainImageFile)
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
    val dest: Path = destObject.resolve(ImgAssetsDefault.MainImageFile)
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

object ImgAssetsDefault {
  private val MainImageFile = "index.jpeg"
  private val w640ImageFile = "index-w-640.jpeg"

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
