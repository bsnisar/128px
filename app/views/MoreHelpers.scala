package views

import views.html.helper.FieldConstructor
import views.html.myFieldConstructorTemplate

object MoreHelpers {
  implicit val noLabel: FieldConstructor = FieldConstructor(myFieldConstructorTemplate.f)

}
