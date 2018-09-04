package de.welt.contentapi.menu.models

import de.welt.contentapi.core.models.ApiReference
import org.scalatestplus.play.PlaySpec

class ApiMenuTest extends PlaySpec {

  "Api Menu" must {

    val menuLink = ApiMenuLink(reference = ApiReference(label = Some("foo"), href = Some("bar")))

    "is not empty when primary menu contains links" in {
      ApiMenu(primaryMenu = Seq(menuLink)).isEmpty mustBe false
    }

    "is not empty when secondary menu contains links" in {
      ApiMenu(secondaryMenu = Seq(menuLink)).isEmpty mustBe false
    }

    "is empty when primary and secondary menu contains no links" in {
      ApiMenu(primaryMenu = Nil, secondaryMenu = Nil).isEmpty mustBe true
    }

  }

}
