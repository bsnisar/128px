package controllers

import play.api.data._
import play.api.data.Forms._

object MyForms {
  case class SearchTerm(value: String)

  val searchForm: Form[SearchTerm] = Form(
    mapping(
      "value" -> text,
    )(SearchTerm.apply)(SearchTerm.unapply)
  )
}
