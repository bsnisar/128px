package photos.assets

import java.util.Base64

import com.google.common.base.Charsets
import com.google.common.hash.Hashing

case class ImgAssetKey(value: String)

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
