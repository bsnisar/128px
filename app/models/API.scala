package models

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import julienrf.json.derived
import play.api.libs.json.Json.fromJson
import play.api.libs.json._

import scala.collection.IndexedSeq
import scala.util.Try
import Keyword._

object Hits {

  type Photos = Hits[Photo]


  implicit def rHits[T](implicit fmt: Reads[T]): Reads[Hits[T]] = new Reads[Hits[T]] {
    override def reads(json: JsValue): JsResult[Hits[T]] = JsResult.fromTry(Try {
      val root = json \ "root"
      new Hits[T](
        (root \ "fields" \ "totalCount").asOpt[Long].getOrElse(-1L),
        (root \ "children") match {
          case JsDefined(value) => value match {
            case JsArray(ts) => ts.map(t => fromJson(t)(rHit(fmt)).get)
            case _ => throw new IllegalArgumentException("children MUST be a list")
          }
          case _: JsUndefined =>
            IndexedSeq.empty[Hit[T]]
        }
      )
    })
  }

  implicit def rHit[T](implicit fmt: Reads[T]): Reads[Hit[T]] = new Reads[Hit[T]] {
    override def reads(json: JsValue): JsResult[Hit[T]] = JsResult.fromTry(Try {
      new Hit[T](
        (json \ "relevance").as[Double],
        (json \ "fields").as[T]
      )
    })
  }
}


case class Hits[V](total: Long, children: IndexedSeq[Hit[V]])
case class Hit[V](relevance: Double, value: V)


case class Embedding(tokens: Array[Float])
object Embedding {

  implicit lazy val EmbeddingReads: Reads[Embedding] = new Reads[Embedding] {
    override def reads(json: JsValue): JsResult[Embedding] = JsResult.fromTry(Try {
      val cells = json \ "cells"
      val address = cells \\ "address"
      val values = cells \\ "value"
      val tokens = Array.ofDim[Float](128)
      address.zip(values).foreach {
        case (o: JsObject, JsNumber(value)) =>
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



/**
 * Stashee base view.
 */
case class Photo(guid: String,
                 binaryGuid: String,
                 description: Option[String],
                 publishTs: Instant,
                 emb: Option[Embedding] = None,
                 keywords: Map[String,Keyword] = Map.empty,
                 meta: Option[Metadata] = None) {

  def userKeywords(take: Int = 25): Seq[(String,Keyword)] = keywords
    .toSeq.take(take).filter { case (_,k) => k.suggestedByUser == 1 }

  def publishTsFmt(): String = {
    val l = LocalDateTime.ofInstant(publishTs, ZoneOffset.UTC)
    l.format(Photo.Fmt)
  }

  def href: Option[String] = meta.flatMap {
    case MetadataUnsplash(_, photoUrl, _) => Some(photoUrl)
    case _ => None
  }
}

object Photo {
  val Fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM, yyyy")

  implicit lazy val PhotoReads: Reads[Photo] = new Reads[Photo] {
    override def reads(json: JsValue): JsResult[Photo] = JsResult.fromTry(Try {
      Photo(
        (json \ "guid").as[String],
        (json \ "binaryGuid").as[String],
        (json \ "description").asOpt[String],
        Instant.ofEpochSecond((json \ "publishTs").as[Long]),
        (json \ "imgEmbedding").asOpt[Embedding],
        (json \ "keywords").validate[Map[String,Keyword]].getOrElse(Map.empty),
        (json \ "metadata").asOpt[Metadata]
      )
    })
  }
}


case class Keyword(aiServiceConfidence1: Double, suggestedByUser: Int)

object Keyword {
  implicit lazy val KeywordR: Reads[Keyword] = Json.reads[Keyword]
}

/**
 * Photo with more like this.
 * @param item current photo
 * @param moreLikeThis more like this
 */
case class SimilarPhotosView(item: Photo, moreLikeThis: IndexedSeq[Photo] = IndexedSeq.empty)
