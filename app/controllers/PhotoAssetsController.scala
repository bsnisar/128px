package controllers

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

import vespa.{ImgAsset, ImgAssetKey, ImgAssetPath, ImgAssets, ImageKind}
import controllers.Assets.parseModifiedDate
import javax.inject._
import play.api.mvc.ResponseHeader.basicDateFormatPattern
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class PhotoAssetsController @Inject()(val controllerComponents: ControllerComponents,
                                      stash: ImgAssets)(implicit ec: ExecutionContext) extends BaseController {

  private val httpDateFormat: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern(basicDateFormatPattern + " 'GMT'")
      .withLocale(java.util.Locale.ENGLISH)
      .withZone(ZoneOffset.UTC)

  def image(id: String, size: String = null): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val path = ImgAssetPath(ImgAssetKey(id), ImageKind.from(size).orElse(Some(ImageKind.Full)))
    for {
      binary <- stash.get(path)
    } yield {
      binary match {
        case Some(asset) =>
          val source = asset.stream
          // produce a range request if it necessary
          val result = RangeResult.ofSource(
            asset.length,
            source,
            request.headers.get(RANGE),
            None,
            Option(asset.mimeType)
          )
          maybeNotModified(request, asset).getOrElse {
            cacheableResult(
              asset,
              asEncodedResult(result, asset)
            )
          }
        case None => NotFound(s"no asset: $id")
      }
    }
  }

  private def maybeNotModified(request: RequestHeader, assetInfo: ImgAsset): Option[Result] = {
    // First check etag. Important, if there is an If-None-Match header, we MUST not check the
    // If-Modified-Since header, regardless of whether If-None-Match matches or not. This is in
    // accordance with section 14.26 of RFC2616.
    request.headers.get(IF_MODIFIED_SINCE) flatMap { ifModifiedSinceStr =>
      val ifModifiedSinceDate = parseModifiedDate(ifModifiedSinceStr)
      for {
        lastModify <- assetInfo.lastModified
        assetInfoTs = Date.from(lastModify)
        ifModifiedSince <- ifModifiedSinceDate
        if !assetInfoTs.after(ifModifiedSince)
      } yield {
        NotModified
      }
    }
  }

  private def cacheableResult[A <: Result](assetInfo: ImgAsset, r: A): Result = {
    def addHeaderIfValue(name: String, maybeValue: Option[String], response: Result): Result = {
      maybeValue.fold(response)(v => response.withHeaders(name -> v))
    }

    // val r1 = addHeaderIfValue(ETAG, assetInfo.etag, r)
    val r2 = addHeaderIfValue(LAST_MODIFIED, assetInfo.lastModified.map(r => httpDateFormat.format(r)), r)
    r2
  }

  private def asEncodedResult(response: Result, assetInfo: ImgAsset): Result = {
    response.withHeaders(VARY -> ACCEPT_ENCODING, CONTENT_ENCODING -> assetInfo.mimeType)
  }
}
