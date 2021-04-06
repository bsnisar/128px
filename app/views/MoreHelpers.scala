package views

import views.html.helper.FieldConstructor
import views.html.simpleInput

object MoreHelpers {
  implicit val noLabel: FieldConstructor = FieldConstructor(simpleInput.f)

}
