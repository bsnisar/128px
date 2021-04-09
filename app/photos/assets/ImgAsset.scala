package photos.assets

import java.io.{File, InputStream}
import java.nio.file.{Files => JFiles}
import java.time.Instant

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.Try

trait ImgAsset {
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