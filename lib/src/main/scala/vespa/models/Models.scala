package vespa.models

import java.time.Instant

import julienrf.json.derived
import play.api.libs.json._

import scala.util.Try

/**
 * Photo with more like this.
 * @param item current photo
 * @param moreLikeThis more like this
 */
case class PhotosSimilar(item: Photo, moreLikeThis: Seq[Photo] = Seq.empty)



/**
 * Stashee base view.
 */
case class Photo(guid: String,
                 binaryGuid: String,
                 description: Option[String],
                 publishTs: Instant,
                 emb: Option[Embedding] = None)


object Photo {
  implicit lazy val PhotoFmt: Reads[Photo] = new Reads[Photo] {
    override def reads(json: JsValue): JsResult[Photo] = JsResult.fromTry(Try {
      Photo(
        (json \ "guid").as[String],
        (json \ "binaryGuid").as[String],
        (json \ "description").asOpt[String],
        Instant.ofEpochSecond((json \ "publishTs").as[Long]),
        (json \ "imgEmbedding").asOpt[Embedding],
      )
    })
  }
}

/**
 * Dense vector.
 * @param tokens tokens
 */
case class Embedding(tokens: Array[Float])

object Embedding {

  implicit lazy val PhotoFmt: Reads[Embedding] = new Reads[Embedding] {
    override def reads(json: JsValue): JsResult[Embedding] = JsResult.fromTry(Try {
      val cells = json \ "cells"
      val address = cells \\ "address"
      val values = cells \\ "value"
      val tokens = Array.ofDim[Float](128)
      address.zip(values).foreach {
        case (o:JsObject, JsNumber(value)) =>
          val idx = o("x").as[String].toInt
          tokens(idx) = value.toFloat
        case no => throw new IllegalArgumentException(s"no: $no")
      }

      Embedding(tokens)
    })
  }

  def fromJson(s: String): Try[Embedding] = Try {
    Json.parse(s) match {
      case JsArray(value) =>
        var i = 0
        Embedding(value.map {
          case JsNumber(v) => i += 1; v.toFloat
          case d => throw new IllegalArgumentException(s"$i : unknown $d")
        }.toArray)
      case un => throw new IllegalArgumentException(s"unknown $un")
    }
  }

}


/**
 * Metadata of the photo.
 */
sealed trait Metadata
case class MetadataUnsplash(id: String, photoUrl: String, photoImgUrl: String) extends Metadata

object Metadata {
  implicit lazy val MetadataUnsplashFmt: OFormat[MetadataUnsplash] = Json.format[MetadataUnsplash]
  implicit lazy val MetadataFmt: OFormat[Metadata] = derived.oformat[Metadata]()
}

