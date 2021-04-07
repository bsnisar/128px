package controllers

import play.api.data._
import play.api.data.Forms._

object MyForms {
  case class SearchTerm(value: String)
  case class SearchUrl(url: String)

  val searchForm: Form[SearchTerm] = Form(
    mapping(
      "value" -> text,
    )(SearchTerm.apply)(SearchTerm.unapply)
  )

  val searchByUrlForm: Form[SearchUrl] = Form(
    mapping(
      "url" -> text,
    )(SearchUrl.apply)(SearchUrl.unapply)
  )
}
