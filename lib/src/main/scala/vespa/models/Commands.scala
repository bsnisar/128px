package vespa.models

import java.time.Instant
import java.util.{Base64, UUID}

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsObject, JsValue, Json}
import vespa.models.FeedCommand.FeedEntry.assign
import vespa.{ImgAssetKey, BinaryContent}

object FeedCommand {
  case class Payload(id: String, json: JsValue)
  case class FeedEntry(json: (String,JsValue))

  object FeedEntry {

    /**
     * ```json
     * {
     *   "update": "id:mynamespace:people::d_duck",
     *   "fields": {
     *       "contacts{\"Uncle Scrooge\"}": {
     *          "assign": {
     *            "phone_number": "555-123-4567",
     *            "email": "number_one_dime_luvr1877@example.com"
     *          }
     *       }
     *   }
     * }
     * ```
     */
    def assignEntry(field: String, entryKey: String, values: Map[String,JsValueWrapper]): FeedEntry = {
      val key = String.format("%s{\"%s\"}", field, entryKey)
      FeedEntry(key -> Json.obj(
        "assign" -> Json.obj(values.toSeq: _*)
      ))
    }

    def assign(field: String, value: JsValueWrapper): FeedEntry =
      FeedEntry(field -> Json.obj("assign" -> value))


    /**
     * Format for indexed tensors.
     * "tensorfield": {
     * "cells": [
     *     { "address": { "x": "a", "y": "0" }, "value": 2.0 },
     *     { "address": { "x": "a", "y": "1" }, "value": 3.0 },
     *     { "address": { "x": "b", "y": "0" }, "value": 4.0 },
     *     { "address": { "x": "b", "y": "1" }, "value": 5.0 }
     *   ]
     * }
     *
     * @param field field
     */
    def tensor(field: String, e: Embedding): FeedEntry = {
      var i = 0
      def getNext: Int = { var tmp = i; i += 1; tmp }
      val feed = Json.obj(
        "cells" -> e.tokens.map(token => Json.obj(
          "address" -> Json.obj("x" -> getNext), "value" -> token
        ))
      )

      FeedEntry(field -> Json.obj("assign" -> feed))
    }
  }
}

trait FeedCommand {

  /**
   * Command uniq identifier.
   */
  val uuid: String = UUID.randomUUID().toString

  /**
   * Feed.
   *
   * @return a feed obj
   */
  def feed: Option[FeedCommand.Payload] = None
}

object Stash {
  //noinspection UnstableApiUsage
  object identity {
    def hash(s: String): String = Hashing.murmur3_128().hashString(s, Charsets.UTF_8).toString
  }
}


/**
 * Post new stashee entity.
 */
case class StockCommandAdd
(guid: String,
 description: String,
 publishTs: Instant,
 binaryGuid: ImgAssetKey,
 binary: BinaryContent,
 lastUpdateTs: Instant = Instant.now(),
 createTs: Instant = Instant.now(),
 metadata: Metadata,
 download: Boolean = true,
) extends FeedCommand {

  override def feed: Option[FeedCommand.Payload] = {
    val payload = Json.obj(
      "fields" -> Json.obj(
        "guid" -> guid,
        "description" -> description,
        "binaryGuid" -> binaryGuid.value,
        "publishTs" -> publishTs.getEpochSecond,
        "lastUpdateTs" -> lastUpdateTs.getEpochSecond,
        "createTs" -> createTs.getEpochSecond,
        "metadata" -> new String(Base64.getEncoder.encode(Json.toBytes(Json.toJson(metadata))))
      )
    )
    Some(FeedCommand.Payload(guid, payload))
  }
}

/**
 * Post new stashee entity.
 */
case class StockCommandUpdate
(guid: String,
 fields: Seq[FeedCommand.FeedEntry]
) extends FeedCommand {

  override def feed: Option[FeedCommand.Payload] = {
    val now = Instant.now().getEpochSecond
    val props = JsObject(fields.map(_.json)) + assign("lastUpdateTs", now).json
    val payload = Json.obj("fields" -> props)
    Some(FeedCommand.Payload(guid, payload))
  }
}



