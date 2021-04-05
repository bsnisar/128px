package vespa.api

import play.api.libs.json.Json.fromJson
import play.api.libs.json._

import scala.collection.IndexedSeq
import scala.util.Try


object Reads {

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
          case undefined: JsUndefined =>
            throw new IllegalArgumentException(s"${undefined.error}")
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

case class Embedding(tokens: Array[Float])

case class Hits[V](total: Long, children: IndexedSeq[Hit[V]])

case class Hit[V](relevance: Double, value: V)

